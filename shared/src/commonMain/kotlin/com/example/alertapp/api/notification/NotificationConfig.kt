package com.example.alertapp.api.notification

import com.example.alertapp.api.ApiConfig
import com.example.alertapp.models.NotificationPriority
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/**
 * Configuration for notification-related API endpoints and settings.
 */
data class NotificationConfig(
    override val apiKey: String,
    override val baseUrl: String = "https://api.example.com/notifications/v1",
    override val timeout: Long = 30_000,
    val defaultPriority: NotificationPriority = NotificationPriority.DEFAULT,
    val maxBatchSize: Int = 10,
    val updateInterval: Duration = 30.seconds,
    val cacheExpiration: Duration = 5.minutes,
    val retryAttempts: Int = 3,
    val retryDelay: Duration = 1.seconds,
    val filters: NotificationFilters = NotificationFilters()
) : ApiConfig

/**
 * Filters for notification queries.
 */
data class NotificationFilters(
    val priorities: Set<NotificationPriority> = emptySet(),
    val minTimestamp: Long? = null,
    val maxTimestamp: Long? = null,
    val searchQuery: String? = null
)
