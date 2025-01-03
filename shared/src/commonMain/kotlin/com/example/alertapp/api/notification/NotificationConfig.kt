package com.example.alertapp.api.notification

import com.example.alertapp.models.NotificationPriority
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/**
 * Configuration for notification-related API endpoints and settings.
 * 
 * @property apiKey API key for authentication
 * @property baseUrl Base URL for the notification API
 * @property timeout Timeout duration in milliseconds
 * @property defaultPriority Default priority for notifications
 * @property maxBatchSize Maximum number of notifications to fetch in a single request
 * @property updateInterval Interval between notification updates
 * @property cacheExpiration Duration after which cached notifications expire
 * @property retryAttempts Number of retry attempts for failed requests
 * @property retryDelay Delay between retry attempts
 * @property filters Default filters for notification queries
 */
data class NotificationConfig(
    val apiKey: String,
    val baseUrl: String = "https://api.example.com/notifications/v1",
    val timeout: Long = 30_000,
    val defaultPriority: NotificationPriority = NotificationPriority.DEFAULT,
    val maxBatchSize: Int = 10,
    val updateInterval: Duration = 30.seconds,
    val cacheExpiration: Duration = 5.minutes,
    val retryAttempts: Int = 3,
    val retryDelay: Duration = 1.seconds,
    val filters: NotificationFilters = NotificationFilters()
) {
    fun toMap(): Map<String, String> = buildMap {
        put("apiKey", apiKey)
        put("baseUrl", baseUrl)
        put("timeout", timeout.toString())
        put("defaultPriority", defaultPriority.name)
        put("maxBatchSize", maxBatchSize.toString())
        put("updateInterval", updateInterval.inWholeMilliseconds.toString())
        put("cacheExpiration", cacheExpiration.inWholeMilliseconds.toString())
        put("retryAttempts", retryAttempts.toString())
        put("retryDelay", retryDelay.inWholeMilliseconds.toString())
    }
}

/**
 * Filters for notification queries.
 * 
 * @property priorities Set of notification priorities to filter by
 * @property minTimestamp Minimum timestamp for notifications
 * @property maxTimestamp Maximum timestamp for notifications
 * @property searchQuery Search query for filtering notifications
 */
data class NotificationFilters(
    val priorities: Set<NotificationPriority> = emptySet(),
    val minTimestamp: Long? = null,
    val maxTimestamp: Long? = null,
    val searchQuery: String? = null
)
