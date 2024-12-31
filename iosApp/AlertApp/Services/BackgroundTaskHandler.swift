import Foundation
import BackgroundTasks
import shared

class BackgroundTaskHandler {
    static let shared = BackgroundTaskHandler()
    private let weatherProcessor: IosWeatherAlertProcessor
    
    private init() {
        self.weatherProcessor = AppState().weatherProcessor
    }
    
    func registerBackgroundTasks() {
        BGTaskScheduler.shared.register(
            forTaskWithIdentifier: "com.example.alertapp.refresh",
            using: nil
        ) { task in
            self.handleAppRefresh(task: task as! BGAppRefreshTask)
        }
        
        BGTaskScheduler.shared.register(
            forTaskWithIdentifier: "com.example.alertapp.processing",
            using: nil
        ) { task in
            self.handleWeatherProcessing(task: task as! BGProcessingTask)
        }
    }
    
    func scheduleAppRefresh() {
        let request = BGAppRefreshTaskRequest(identifier: "com.example.alertapp.refresh")
        request.earliestBeginDate = Date(timeIntervalSinceNow: 15 * 60) // 15 minutes
        
        do {
            try BGTaskScheduler.shared.submit(request)
        } catch {
            print("Could not schedule app refresh: \(error)")
        }
    }
    
    func scheduleWeatherProcessing() {
        let request = BGProcessingTaskRequest(identifier: "com.example.alertapp.processing")
        request.requiresNetworkConnectivity = true
        request.requiresExternalPower = false
        request.earliestBeginDate = Date(timeIntervalSinceNow: 60 * 60) // 1 hour
        
        do {
            try BGTaskScheduler.shared.submit(request)
        } catch {
            print("Could not schedule weather processing: \(error)")
        }
    }
    
    private func handleAppRefresh(task: BGAppRefreshTask) {
        scheduleAppRefresh() // Schedule the next refresh
        
        let queue = OperationQueue()
        queue.maxConcurrentOperationCount = 1
        
        queue.addOperation {
            self.weatherProcessor.processAlerts { result, error in
                if let error = error {
                    print("Background refresh failed: \(error.localizedDescription)")
                    task.setTaskCompleted(success: false)
                } else {
                    NotificationHandler.shared.scheduleNotificationsForNewAlerts(result as? [Alert] ?? [])
                    task.setTaskCompleted(success: true)
                }
            }
        }
        
        // Ensure the task is marked as complete
        task.expirationHandler = {
            queue.cancelAllOperations()
        }
    }
    
    private func handleWeatherProcessing(task: BGProcessingTask) {
        scheduleWeatherProcessing() // Schedule the next processing
        
        let queue = OperationQueue()
        queue.maxConcurrentOperationCount = 1
        
        queue.addOperation {
            self.weatherProcessor.processHistoricalData { success in
                task.setTaskCompleted(success: success)
            }
        }
        
        task.expirationHandler = {
            queue.cancelAllOperations()
        }
    }
}
