import Foundation
import UserNotifications
import shared

class AlertContentFormatter {
    static let shared = AlertContentFormatter()
    
    // Generic alert categories that can cover any type of alert
    enum AlertCategory: String {
        case immediate = "IMMEDIATE"      // For time-critical alerts (weather warnings, stock crashes)
        case scheduled = "SCHEDULED"      // For future events (concerts, sports games)
        case monitoring = "MONITORING"    // For continuous monitoring (price thresholds, news keywords)
        case update = "UPDATE"           // For updates to existing alerts
        case custom = "CUSTOM"           // For user-defined categories
    }
    
    // Priority levels that can apply to any alert type
    enum AlertPriority: Int {
        case critical = 3    // Immediate attention required
        case high = 2       // Important but not urgent
        case medium = 1     // Normal priority
        case low = 0        // Information only
        
        var soundName: String {
            switch self {
            case .critical: return "critical_alert"
            case .high: return "high_priority"
            case .medium: return "notification"
            case .low: return "subtle_alert"
            }
        }
    }
    
    func formatNotificationContent(for alert: Alert) -> UNMutableNotificationContent {
        let content = UNMutableNotificationContent()
        
        // Basic content
        content.title = alert.title
        content.body = alert.description_
        
        // Set sound based on priority
        let priority = determineAlertPriority(alert)
        content.sound = getNotificationSound(for: priority)
        
        // Set category for actions
        content.categoryIdentifier = getAlertCategory(for: alert).rawValue
        
        // Add relevance score based on priority and timing
        content.relevanceScore = calculateRelevanceScore(alert, priority: priority)
        
        // Add thread identifier for grouping related alerts
        content.threadIdentifier = generateThreadIdentifier(for: alert)
        
        // Add rich metadata
        enrichContent(content, with: alert)
        
        return content
    }
    
    private func determineAlertPriority(_ alert: Alert) -> AlertPriority {
        // Determine priority based on alert properties
        if alert.isCritical {
            return .critical
        }
        
        // Check time sensitivity
        let timeToEvent = alert.triggerDate?.timeIntervalSince(Date()) ?? 0
        if timeToEvent > 0 && timeToEvent < 3600 { // Within next hour
            return .high
        }
        
        // Check user preferences (if any)
        if let userPriority = alert.userDefinedPriority {
            return AlertPriority(rawValue: userPriority) ?? .medium
        }
        
        return .medium
    }
    
    private func getNotificationSound(for priority: AlertPriority) -> UNNotificationSound {
        if priority == .critical {
            return UNNotificationSound.defaultCritical
        }
        return UNNotificationSound(named: UNNotificationSoundName(priority.soundName))
    }
    
    private func getAlertCategory(for alert: Alert) -> AlertCategory {
        if alert.isCritical {
            return .immediate
        }
        
        // Check if it's a scheduled event
        if alert.triggerDate != nil {
            return .scheduled
        }
        
        // Check if it's monitoring something
        if alert.isMonitoring ?? false {
            return .monitoring
        }
        
        // Check if it's an update
        if alert.isUpdate ?? false {
            return .update
        }
        
        return .custom
    }
    
    private func calculateRelevanceScore(_ alert: Alert, priority: AlertPriority) -> Double {
        var score = Double(priority.rawValue) * 0.25 // Base score from priority
        
        // Add time-based relevance
        if let triggerDate = alert.triggerDate {
            let timeToEvent = triggerDate.timeIntervalSince(Date())
            if timeToEvent > 0 {
                // Score decreases as time to event increases
                score += 1.0 / (1.0 + log10(timeToEvent/3600)) // Using hours
            }
        }
        
        // Add user interest relevance if available
        if let userInterest = alert.userInterestScore {
            score += Double(userInterest) * 0.25
        }
        
        return min(max(score, 0), 1) // Normalize between 0 and 1
    }
    
    private func generateThreadIdentifier(for alert: Alert) -> String {
        var components: [String] = []
        
        // Add source type
        components.append(alert.sourceType ?? "generic")
        
        // Add category if available
        if let category = alert.category {
            components.append(category)
        }
        
        // Add topic or subject if available
        if let topic = alert.topic {
            components.append(topic)
        }
        
        // Add location identifier if location-based
        if let locationId = alert.location?.id {
            components.append(locationId)
        }
        
        return components.joined(separator: "_")
    }
    
    private func enrichContent(_ content: UNMutableNotificationContent, with alert: Alert) {
        var userInfo: [String: Any] = [
            "alertId": alert.id,
            "sourceType": alert.sourceType ?? "unknown",
            "timestamp": Date().timeIntervalSince1970
        ]
        
        // Add source-specific data
        if let sourceData = alert.sourceData {
            userInfo["sourceData"] = sourceData
        }
        
        // Add location data if available
        if let location = alert.location {
            userInfo["location"] = [
                "id": location.id,
                "name": location.name,
                "latitude": location.latitude,
                "longitude": location.longitude
            ]
        }
        
        // Add user preferences if available
        if let userPrefs = alert.userPreferences {
            userInfo["userPreferences"] = userPrefs
        }
        
        content.userInfo = userInfo
    }
}
