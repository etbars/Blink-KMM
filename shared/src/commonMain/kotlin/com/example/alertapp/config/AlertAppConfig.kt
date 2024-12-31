package com.example.alertapp.config

import kotlinx.serialization.Serializable
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/**
 * Main configuration object for the AlertApp.
 */
@Serializable
data class AlertAppConfig(
    val alertCheck: AlertCheckConfig = AlertCheckConfig(),
    val api: ApiConfig = ApiConfig(),
    val cache: CacheConfig = CacheConfig(),
    val database: DatabaseConfig = DatabaseConfig(),
    val pagination: PaginationConfig = PaginationConfig(),
    val timing: TimingConfig = TimingConfig()
)

/**
 * Configuration for alert checking intervals.
 */
@Serializable
data class AlertCheckConfig(
    val defaultInterval: Duration = 15.minutes,
    val minInterval: Duration = 5.minutes,
    val maxInterval: Duration = 24.hours
)

/**
 * Configuration for API endpoints and keys.
 */
@Serializable
data class ApiConfig(
    val newsApi: ApiEndpointConfig = ApiEndpointConfig(
        baseUrl = "https://newsapi.org/v2/",
        apiKey = "YOUR_NEWS_API_KEY" // Replace with actual key in production
    ),
    val weatherApi: ApiEndpointConfig = ApiEndpointConfig(
        baseUrl = "https://api.openweathermap.org/data/2.5/",
        apiKey = "YOUR_WEATHER_API_KEY" // Replace with actual key in production
    ),
    val priceApi: ApiEndpointConfig = ApiEndpointConfig(
        baseUrl = "https://api.alphavantage.co/",
        apiKey = "YOUR_PRICE_API_KEY" // Replace with actual key in production
    )
)

/**
 * Configuration for an API endpoint.
 */
@Serializable
data class ApiEndpointConfig(
    val baseUrl: String,
    val apiKey: String,
    val timeout: Duration = 30.seconds
)

/**
 * Configuration for caching.
 */
@Serializable
data class CacheConfig(
    val maxSizeBytes: Long = 10L * 1024L * 1024L, // 10 MB
    val maxAge: Duration = 1.hours,
    val maxStale: Duration = 7.days,
    val timeout: Duration = 5.seconds
)

/**
 * Configuration for the database.
 */
@Serializable
data class DatabaseConfig(
    val name: String = "alert_app.db",
    val version: Int = 1
)

/**
 * Configuration for pagination.
 */
@Serializable
data class PaginationConfig(
    val defaultPageSize: Int = 20,
    val maxPageSize: Int = 100,
    val initialPage: Int = 1
)

/**
 * Configuration for timing-related settings.
 */
@Serializable
data class TimingConfig(
    val animationDurationShort: Duration = 150.milliseconds,
    val animationDurationMedium: Duration = 300.milliseconds,
    val animationDurationLong: Duration = 500.milliseconds,
    val networkTimeout: Duration = 30.seconds,
    val cacheTimeout: Duration = 5.seconds
)
