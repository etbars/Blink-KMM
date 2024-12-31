import Foundation
import CoreLocation
import UserNotifications
import shared

class LocationNotificationManager: NSObject, CLLocationManagerDelegate {
    static let shared = LocationNotificationManager()
    private let locationManager = CLLocationManager()
    private var monitoredRegions: [String: CLCircularRegion] = [:]
    private let weatherProcessor: IosWeatherAlertProcessor
    
    private override init() {
        self.weatherProcessor = AppState().weatherProcessor
        super.init()
        setupLocationManager()
    }
    
    private func setupLocationManager() {
        locationManager.delegate = self
        locationManager.desiredAccuracy = kCLLocationAccuracyHundredMeters
        locationManager.allowsBackgroundLocationUpdates = true
        locationManager.pausesLocationUpdatesAutomatically = false
        requestLocationPermissions()
    }
    
    private func requestLocationPermissions() {
        locationManager.requestAlwaysAuthorization()
    }
    
    func startMonitoringWeatherLocations() {
        // Clear existing regions
        for region in locationManager.monitoredRegions {
            locationManager.stopMonitoring(for: region)
        }
        monitoredRegions.removeAll()
        
        // Get weather alert locations from processor
        weatherProcessor.getMonitoredLocations { locations, error in
            guard let locations = locations as? [Location] else { return }
            
            for location in locations {
                self.addGeofence(for: location)
            }
        }
    }
    
    private func addGeofence(for location: Location) {
        let coordinate = CLLocationCoordinate2D(latitude: location.latitude, longitude: location.longitude)
        let radius = location.radius ?? 1000 // Default radius of 1km
        
        let region = CLCircularRegion(
            center: coordinate,
            radius: CLLocationDistance(radius),
            identifier: location.id
        )
        
        region.notifyOnEntry = true
        region.notifyOnExit = true
        
        locationManager.startMonitoring(for: region)
        monitoredRegions[location.id] = region
    }
    
    // MARK: - CLLocationManagerDelegate
    
    func locationManager(_ manager: CLLocationManager, didEnterRegion region: CLRegion) {
        handleRegionEvent(region: region, isEntering: true)
    }
    
    func locationManager(_ manager: CLLocationManager, didExitRegion region: CLRegion) {
        handleRegionEvent(region: region, isEntering: false)
    }
    
    private func handleRegionEvent(region: CLRegion, isEntering: Bool) {
        weatherProcessor.getLocationDetails(id: region.identifier) { location, error in
            guard let location = location else { return }
            
            // Create dynamic notification content
            let content = UNMutableNotificationContent()
            content.title = isEntering ? "Entering Weather Alert Zone" : "Leaving Weather Alert Zone"
            
            // Get current weather for location
            self.weatherProcessor.getCurrentWeather(for: location) { weather, error in
                if let weather = weather {
                    // Create rich notification with weather data
                    content.subtitle = "\(weather.temperature)°C, \(weather.condition)"
                    content.body = self.getWeatherDescription(weather: weather, isEntering: isEntering)
                    
                    // Add weather icon as attachment
                    if let weatherIcon = weather.iconCode {
                        self.attachWeatherIcon(to: content, iconCode: weatherIcon) { modifiedContent in
                            self.scheduleLocationNotification(content: modifiedContent, region: region)
                        }
                    } else {
                        self.scheduleLocationNotification(content: content, region: region)
                    }
                }
            }
        }
    }
    
    private func getWeatherDescription(weather: Weather, isEntering: Bool) -> String {
        if isEntering {
            return "Current conditions: \(weather.condition). " +
                   "Temperature: \(weather.temperature)°C. " +
                   "Wind: \(weather.windSpeed) km/h. " +
                   "Stay alert for weather changes."
        } else {
            return "You're leaving an area with \(weather.condition). " +
                   "New weather alerts will be based on your current location."
        }
    }
    
    private func attachWeatherIcon(to content: UNMutableNotificationContent, iconCode: String, completion: @escaping (UNMutableNotificationContent) -> Void) {
        // Download weather icon from OpenWeatherMap
        let iconUrl = "https://openweathermap.org/img/wn/\(iconCode)@2x.png"
        
        guard let url = URL(string: iconUrl) else {
            completion(content)
            return
        }
        
        let task = URLSession.shared.downloadTask(with: url) { localUrl, _, error in
            guard let localUrl = localUrl else {
                completion(content)
                return
            }
            
            do {
                let tempDir = FileManager.default.temporaryDirectory
                let iconFile = tempDir.appendingPathComponent("\(iconCode).png")
                try? FileManager.default.removeItem(at: iconFile)
                try FileManager.default.moveItem(at: localUrl, to: iconFile)
                
                if let attachment = try? UNNotificationAttachment(
                    identifier: "weatherIcon",
                    url: iconFile,
                    options: [UNNotificationAttachmentOptionsTypeHintKey: "image/png"]
                ) {
                    content.attachments = [attachment]
                }
            } catch {
                print("Error creating notification attachment: \(error)")
            }
            
            completion(content)
        }
        task.resume()
    }
    
    private func scheduleLocationNotification(content: UNMutableNotificationContent, region: CLRegion) {
        // Add category for location-based actions
        content.categoryIdentifier = "LOCATION_ALERT"
        
        // Create trigger (immediate delivery)
        let trigger = UNTimeIntervalNotificationTrigger(timeInterval: 1, repeats: false)
        
        // Create request with unique identifier
        let request = UNNotificationRequest(
            identifier: "location_\(region.identifier)_\(Date().timeIntervalSince1970)",
            content: content,
            trigger: trigger
        )
        
        // Schedule notification
        UNUserNotificationCenter.current().add(request) { error in
            if let error = error {
                print("Error scheduling location notification: \(error)")
            }
        }
    }
    
    func setupLocationNotificationCategories() {
        let viewWeatherAction = UNNotificationAction(
            identifier: "VIEW_WEATHER",
            title: "View Weather Details",
            options: .foreground
        )
        
        let startMonitoringAction = UNNotificationAction(
            identifier: "START_MONITORING",
            title: "Monitor This Location",
            options: []
        )
        
        let stopMonitoringAction = UNNotificationAction(
            identifier: "STOP_MONITORING",
            title: "Stop Monitoring",
            options: [.destructive]
        )
        
        let category = UNNotificationCategory(
            identifier: "LOCATION_ALERT",
            actions: [viewWeatherAction, startMonitoringAction, stopMonitoringAction],
            intentIdentifiers: [],
            options: []
        )
        
        UNUserNotificationCenter.current().getNotificationCategories { categories in
            var updatedCategories = categories
            updatedCategories.insert(category)
            UNUserNotificationCenter.current().setNotificationCategories(updatedCategories)
        }
    }
}
