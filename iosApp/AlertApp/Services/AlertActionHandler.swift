import Foundation
import UserNotifications
import shared

class AlertActionHandler {
    static let shared = AlertActionHandler()
    
    // Standard actions that can apply to any alert type
    enum StandardAction: String {
        case view = "VIEW"              // View alert details
        case snooze = "SNOOZE"          // Snooze the alert
        case share = "SHARE"            // Share the alert
        case modify = "MODIFY"          // Modify alert settings
        case disable = "DISABLE"        // Disable this type of alert
        case openSource = "OPEN_SOURCE" // Open the source (website, app, etc.)
    }
    
    func setupNotificationCategories() {
        var categories: Set<UNNotificationCategory> = []
        
        // Immediate category (for critical alerts)
        let immediateCategory = createCategory(
            identifier: AlertContentFormatter.AlertCategory.immediate.rawValue,
            actions: [
                createAction(StandardAction.view.rawValue, title: "View Now", options: [.foreground, .destructive]),
                createAction(StandardAction.share.rawValue, title: "Share", options: []),
                createAction(StandardAction.disable.rawValue, title: "Disable", options: [.destructive])
            ],
            options: [.hiddenPreviewsShowTitle, .allowAnnouncement]
        )
        categories.insert(immediateCategory)
        
        // Scheduled category (for future events)
        let scheduledCategory = createCategory(
            identifier: AlertContentFormatter.AlertCategory.scheduled.rawValue,
            actions: [
                createAction(StandardAction.view.rawValue, title: "View Details", options: [.foreground]),
                createAction(StandardAction.snooze.rawValue, title: "Remind Later", options: []),
                createAction(StandardAction.modify.rawValue, title: "Modify", options: [])
            ],
            options: []
        )
        categories.insert(scheduledCategory)
        
        // Monitoring category (for continuous alerts)
        let monitoringCategory = createCategory(
            identifier: AlertContentFormatter.AlertCategory.monitoring.rawValue,
            actions: [
                createAction(StandardAction.view.rawValue, title: "View Details", options: [.foreground]),
                createAction(StandardAction.modify.rawValue, title: "Adjust Threshold", options: []),
                createAction(StandardAction.disable.rawValue, title: "Stop Monitoring", options: [.destructive])
            ],
            options: []
        )
        categories.insert(monitoringCategory)
        
        // Update category
        let updateCategory = createCategory(
            identifier: AlertContentFormatter.AlertCategory.update.rawValue,
            actions: [
                createAction(StandardAction.view.rawValue, title: "View Update", options: [.foreground]),
                createAction(StandardAction.openSource.rawValue, title: "Go to Source", options: []),
                createAction(StandardAction.disable.rawValue, title: "Disable Updates", options: [])
            ],
            options: []
        )
        categories.insert(updateCategory)
        
        // Custom category (for user-defined alerts)
        let customCategory = createCategory(
            identifier: AlertContentFormatter.AlertCategory.custom.rawValue,
            actions: [
                createAction(StandardAction.view.rawValue, title: "View", options: [.foreground]),
                createAction(StandardAction.modify.rawValue, title: "Edit Alert", options: []),
                createAction(StandardAction.disable.rawValue, title: "Remove", options: [.destructive])
            ],
            options: []
        )
        categories.insert(customCategory)
        
        // Register all categories
        UNUserNotificationCenter.current().setNotificationCategories(categories)
    }
    
    private func createAction(_ identifier: String, title: String, options: UNNotificationActionOptions) -> UNNotificationAction {
        UNNotificationAction(identifier: identifier, title: title, options: options)
    }
    
    private func createCategory(identifier: String, actions: [UNNotificationAction], options: UNNotificationCategoryOptions) -> UNNotificationCategory {
        UNNotificationCategory(
            identifier: identifier,
            actions: actions,
            intentIdentifiers: [],
            hiddenPreviewsBodyPlaceholder: "New Alert",
            options: options
        )
    }
    
    func handleNotificationResponse(_ response: UNNotificationResponse, completion: @escaping () -> Void) {
        let userInfo = response.notification.request.content.userInfo
        let alertId = userInfo["alertId"] as? String ?? ""
        let sourceType = userInfo["sourceType"] as? String ?? "unknown"
        
        switch response.actionIdentifier {
        case StandardAction.view.rawValue:
            NotificationCenter.default.post(
                name: NSNotification.Name("ViewAlertDetails"),
                object: nil,
                userInfo: ["alertId": alertId, "sourceType": sourceType]
            )
            
        case StandardAction.snooze.rawValue:
            handleSnooze(alertId: alertId, sourceType: sourceType)
            
        case StandardAction.share.rawValue:
            handleShare(userInfo: userInfo)
            
        case StandardAction.modify.rawValue:
            NotificationCenter.default.post(
                name: NSNotification.Name("ModifyAlert"),
                object: nil,
                userInfo: ["alertId": alertId, "sourceType": sourceType]
            )
            
        case StandardAction.disable.rawValue:
            handleDisable(alertId: alertId, sourceType: sourceType)
            
        case StandardAction.openSource.rawValue:
            handleOpenSource(userInfo: userInfo)
            
        case UNNotificationDefaultActionIdentifier:
            // Default action when notification is tapped
            NotificationCenter.default.post(
                name: NSNotification.Name("ViewAlertDetails"),
                object: nil,
                userInfo: ["alertId": alertId, "sourceType": sourceType]
            )
            
        default:
            break
        }
        
        completion()
    }
    
    private func handleSnooze(alertId: String, sourceType: String) {
        // Create a snoozed reminder
        let content = UNMutableNotificationContent()
        content.title = "Reminder"
        content.body = "You have a snoozed alert to review"
        content.sound = .default
        
        // Snooze for 1 hour by default
        let trigger = UNTimeIntervalNotificationTrigger(timeInterval: 3600, repeats: false)
        let request = UNNotificationRequest(
            identifier: "snooze_\(alertId)",
            content: content,
            trigger: trigger
        )
        
        UNUserNotificationCenter.current().add(request)
    }
    
    private func handleShare(userInfo: [AnyHashable: Any]) {
        guard let alertTitle = userInfo["title"] as? String,
              let description = userInfo["description"] as? String else {
            return
        }
        
        let text = "\(alertTitle)\n\(description)"
        
        // Post notification to trigger share sheet in UI
        NotificationCenter.default.post(
            name: NSNotification.Name("ShareAlert"),
            object: nil,
            userInfo: ["text": text]
        )
    }
    
    private func handleDisable(alertId: String, sourceType: String) {
        // Post notification to disable alerts of this type
        NotificationCenter.default.post(
            name: NSNotification.Name("DisableAlerts"),
            object: nil,
            userInfo: ["alertId": alertId, "sourceType": sourceType]
        )
    }
    
    private func handleOpenSource(userInfo: [AnyHashable: Any]) {
        if let sourceData = userInfo["sourceData"] as? [String: Any],
           let sourceUrl = sourceData["url"] as? String,
           let url = URL(string: sourceUrl) {
            // Post notification to open URL in app or browser
            NotificationCenter.default.post(
                name: NSNotification.Name("OpenSource"),
                object: nil,
                userInfo: ["url": url]
            )
        }
    }
}
