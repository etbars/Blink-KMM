import SwiftUI
import shared
import BackgroundTasks
import UserNotifications

@main
struct AlertAppApp: App {
    @StateObject private var appState = AppState()
    
    init() {
        // Register background tasks
        BackgroundTaskHandler.shared.registerBackgroundTasks()
        
        // Setup notifications
        NotificationHandler.shared.requestAuthorization()
        NotificationHandler.shared.setupNotificationCategories()
    }
    
    var body: some Scene {
        WindowGroup {
            ContentView()
                .environmentObject(appState)
                .onAppear {
                    // Schedule background tasks
                    BackgroundTaskHandler.shared.scheduleAppRefresh()
                    BackgroundTaskHandler.shared.scheduleWeatherProcessing()
                }
        }
    }
}

class AppState: ObservableObject {
    @Published var weatherProcessor: IosWeatherAlertProcessor
    @Published var locationProvider: IosLocationProvider
    
    init() {
        self.locationProvider = IosLocationProvider()
        self.weatherProcessor = IosWeatherAlertProcessor(locationProvider: locationProvider)
    }
    
    func handlePushNotification(_ userInfo: [AnyHashable: Any]) {
        // Process push notification data
        weatherProcessor.processAlerts { result, error in
            if let alerts = result as? [Alert] {
                NotificationHandler.shared.scheduleNotificationsForNewAlerts(alerts)
            }
        }
    }
}
