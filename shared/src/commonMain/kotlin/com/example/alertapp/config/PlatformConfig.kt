package com.example.alertapp.config

import kotlinx.serialization.Serializable

/**
 * Platform-specific configuration settings.
 */
@Serializable
data class PlatformConfig(
    val notification: NotificationConfig = NotificationConfig(),
    val permissions: PermissionsConfig = PermissionsConfig(),
    val storage: StorageConfig = StorageConfig()
)

/**
 * Configuration for notifications.
 */
@Serializable
data class NotificationConfig(
    val channelId: String = "alert_notifications",
    val channelName: String = "Alert Notifications",
    val channelDescription: String = "Notifications for triggered alerts"
)

/**
 * Configuration for permissions.
 */
@Serializable
data class PermissionsConfig(
    val locationPermissionCode: Int = 1001,
    val notificationPermissionCode: Int = 1002,
    val signInPermissionCode: Int = 1003
)

/**
 * Configuration for storage.
 */
@Serializable
data class StorageConfig(
    val preferencesName: String = "alert_app_preferences",
    val themeModePrefKey: String = "theme_mode",
    val notificationEnabledPrefKey: String = "notifications_enabled",
    val lastSyncPrefKey: String = "last_sync"
)
