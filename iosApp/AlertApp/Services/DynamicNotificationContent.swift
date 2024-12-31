import Foundation
import UserNotifications
import shared

class DynamicNotificationContent {
    static let shared = DynamicNotificationContent()
    private let weatherProcessor: IosWeatherAlertProcessor
    
    private init() {
        self.weatherProcessor = AppState().weatherProcessor
    }
    
    func enrichNotificationContent(_ content: UNMutableNotificationContent, for alert: Alert) -> UNMutableNotificationContent {
        // Add dynamic time-based greeting
        let greeting = getTimeBasedGreeting()
        content.title = "\(greeting) - \(content.title)"
        
        // Add weather emoji based on conditions
        if let weatherType = alert.weatherType {
            content.subtitle = "\(getWeatherEmoji(for: weatherType)) \(content.subtitle)"
        }
        
        // Add dynamic severity level
        if let severity = alert.severity {
            let severityIndicator = getSeverityIndicator(level: severity)
            content.body = "\(severityIndicator)\n\(content.body)"
        }
        
        // Add dynamic summary
        content.body += "\n\n" + generateDynamicSummary(for: alert)
        
        return content
    }
    
    private func getTimeBasedGreeting() -> String {
        let hour = Calendar.current.component(.hour, from: Date())
        switch hour {
        case 0..<5:
            return "Night Alert"
        case 5..<12:
            return "Morning Alert"
        case 12..<17:
            return "Afternoon Alert"
        case 17..<22:
            return "Evening Alert"
        default:
            return "Late Night Alert"
        }
    }
    
    private func getWeatherEmoji(for weatherType: WeatherType) -> String {
        switch weatherType {
        case .sunny:
            return "☀️"
        case .rain:
            return "🌧"
        case .snow:
            return "❄️"
        case .wind:
            return "💨"
        case .storm:
            return "⛈"
        case .tornado:
            return "🌪"
        case .hurricane:
            return "🌀"
        case .flood:
            return "🌊"
        default:
            return "⚠️"
        }
    }
    
    private func getSeverityIndicator(level: Int) -> String {
        let indicator = "⚠️"
        return String(repeating: indicator, count: level)
    }
    
    private func generateDynamicSummary(for alert: Alert) -> String {
        var summary = ["Quick Summary:"]
        
        // Add time-based context
        let timeUntilExpiry = alert.expiryDate.timeIntervalSince(Date())
        if timeUntilExpiry > 0 {
            let hours = Int(timeUntilExpiry / 3600)
            summary.append("• Valid for next \(hours) hours")
        }
        
        // Add location context if available
        if let location = alert.location {
            summary.append("• Affecting \(location.name)")
        }
        
        // Add weather-specific advice
        if let weatherType = alert.weatherType {
            summary.append("• \(getWeatherAdvice(for: weatherType))")
        }
        
        // Add historical context
        weatherProcessor.getHistoricalAlerts(type: alert.type) { alerts, error in
            if let alerts = alerts as? [Alert] {
                let similarAlerts = alerts.filter { $0.type == alert.type }
                if !similarAlerts.isEmpty {
                    summary.append("• \(similarAlerts.count) similar alerts in past week")
                }
            }
        }
        
        return summary.joined(separator: "\n")
    }
    
    private func getWeatherAdvice(for weatherType: WeatherType) -> String {
        switch weatherType {
        case .rain:
            return "Remember to carry an umbrella"
        case .snow:
            return "Drive carefully on icy roads"
        case .wind:
            return "Secure loose outdoor items"
        case .storm:
            return "Stay indoors if possible"
        case .tornado:
            return "Seek immediate shelter"
        case .hurricane:
            return "Follow evacuation orders"
        case .flood:
            return "Avoid flood-prone areas"
        default:
            return "Stay weather-aware"
        }
    }
    
    func addDynamicActions(to content: UNMutableNotificationContent, for alert: Alert) {
        var categoryIdentifier = content.categoryIdentifier
        
        // Add time-sensitive options
        if alert.expiryDate.timeIntervalSince(Date()) < 3600 { // Less than 1 hour
            categoryIdentifier += "_URGENT"
        }
        
        // Add weather-specific options
        if let weatherType = alert.weatherType {
            categoryIdentifier += "_\(weatherType.description())"
        }
        
        content.categoryIdentifier = categoryIdentifier
    }
}
