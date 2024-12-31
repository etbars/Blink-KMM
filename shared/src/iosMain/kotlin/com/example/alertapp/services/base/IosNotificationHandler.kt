package com.example.alertapp.services.base

import platform.UserNotifications.*
import platform.Foundation.NSError
import platform.darwin.NSObject
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.datetime.Clock
import kotlin.coroutines.resume

class IosNotificationHandler : NotificationHandler {
    private val notificationCenter = UNUserNotificationCenter.currentNotificationCenter()

    @OptIn(ExperimentalForeignApi::class)
    override suspend fun sendNotification(title: String, message: String) {
        // Request permission if not already granted
        requestNotificationPermission()

        // Create notification content
        val content = UNMutableNotificationContent().apply {
            setTitle(title)
            setBody(message)
            setSound(UNNotificationSound.defaultSound)
        }

        // Create request
        val request = UNNotificationRequest.requestWithIdentifier(
            identifier = Clock.System.now().epochSeconds.toString(),
            content = content,
            trigger = null // Deliver immediately
        )

        // Add request to notification center
        return suspendCancellableCoroutine { continuation ->
            notificationCenter.addNotificationRequest(request) { error ->
                if (error != null) {
                    println("Failed to schedule notification: ${error.localizedDescription}")
                }
                continuation.resume(Unit)
            }
        }
    }

    override suspend fun cancelNotification(id: String) {
        notificationCenter.removePendingNotificationRequestsWithIdentifiers(listOf(id))
        notificationCenter.removeDeliveredNotificationsWithIdentifiers(listOf(id))
    }

    @OptIn(ExperimentalForeignApi::class)
    private suspend fun requestNotificationPermission() {
        return suspendCancellableCoroutine { continuation ->
            notificationCenter.requestAuthorizationWithOptions(
                UNAuthorizationOptionSound.or(
                    UNAuthorizationOptionAlert.or(
                        UNAuthorizationOptionBadge
                    )
                )
            ) { granted, error ->
                if (error != null) {
                    println("Failed to request notification permission: ${error.localizedDescription}")
                }
                continuation.resume(Unit)
            }
        }
    }
}
