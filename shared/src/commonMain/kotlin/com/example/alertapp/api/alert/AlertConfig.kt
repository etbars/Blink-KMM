package com.example.alertapp.api.alert

import com.example.alertapp.api.ApiConfig
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/**
 * Configuration for alert-related API endpoints and settings.
 */
data class AlertConfig(
    override val apiKey: String,
    override val baseUrl: String = "https://api.example.com/alerts/v1",
    override val timeout: Long = 30_000,
    val defaultCheckInterval: Duration = 15.minutes,
    val updateInterval: Duration = 30.seconds,
    val cacheExpiration: Duration = 5.minutes,
    val retryAttempts: Int = 3,
    val retryDelay: Duration = 1.seconds,
    val maxConcurrentChecks: Int = 5,
    val filters: AlertFilters = AlertFilters()
) : ApiConfig

/**
 * Filters for alert queries.
 */
data class AlertFilters(
    val types: Set<String> = emptySet(),
    val priorities: Set<String> = emptySet(),
    val excludeInactive: Boolean = true,
    val excludeExpired: Boolean = true,
    val minLastTriggered: Duration? = null,
    val maxLastTriggered: Duration? = null
)
