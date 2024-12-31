import Foundation
import UserNotifications
import shared

class NotificationHandler {
    static let shared = NotificationHandler()
    private let alertProcessor: IosAlertProcessor // Generic alert processor
    
    private init() {
        self.alertProcessor = AppState().alertProcessor
        
        // Initialize action handler
        AlertActionHandler.shared.setupNotificationCategories()
    }
    
    func requestAuthorization() {
        let options: UNAuthorizationOptions = [.alert, .badge, .sound, .criticalAlert]
        UNUserNotificationCenter.current().requestAuthorization(options: options) { granted, error in
            if granted {
                self.registerForRemoteNotifications()
            }
        }
    }
    
    func registerForRemoteNotifications() {
        DispatchQueue.main.async {
            UIApplication.shared.registerForRemoteNotifications()
        }
    }
    
    func scheduleNotificationsForNewAlerts(_ alerts: [Alert]) {
        let center = UNUserNotificationCenter.current()
        
        // Remove pending notifications
        center.removeAllPendingNotificationRequests()
        
        for alert in alerts {
            guard alert.status == AlertStatus.active else { continue }
            
            // Format notification content
            let content = AlertContentFormatter.shared.formatNotificationContent(for: alert)
            
            // Create trigger based on alert type
            let trigger = createTrigger(for: alert)
            
            // Create and schedule notification request
            let request = UNNotificationRequest(
                identifier: alert.id,
                content: content,
                trigger: trigger
            )
            
            center.add(request) { error in
                if let error = error {
                    print("Error scheduling notification: \(error)")
                }
            }
        }
    }
    
    private func createTrigger(for alert: Alert) -> UNNotificationTrigger? {
        // If it's a critical alert, trigger immediately
        if alert.isCritical {
            return UNTimeIntervalNotificationTrigger(timeInterval: 1, repeats: false)
        }
        
        // If it has a specific trigger time
        if let triggerDate = alert.triggerDate {
            let components = Calendar.current.dateComponents([.year, .month, .day, .hour, .minute], from: triggerDate)
            return UNCalendarNotificationTrigger(dateMatching: components, repeats: false)
        }
        
        // If it's a recurring alert
        if let interval = alert.repeatInterval {
            return UNTimeIntervalNotificationTrigger(timeInterval: interval, repeats: true)
        }
        
        // Default to immediate trigger
        return UNTimeIntervalNotificationTrigger(timeInterval: 1, repeats: false)
    }
    
    func handleNotificationResponse(_ response: UNNotificationResponse) {
        AlertActionHandler.shared.handleNotificationResponse(response) {
            // Any additional cleanup or logging after handling the response
        }
    }
}
