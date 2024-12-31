package com.example.alertapp.services.base

/**
 * Platform-specific notification handling
 */
interface NotificationHandler {
    /**
     * Send a notification to the user
     */
    suspend fun sendNotification(title: String, message: String)
}
