package com.example.alertapp.api.event

import com.example.alertapp.api.ApiConfig
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/**
 * Configuration for event-related API endpoints and settings.
 */
data class EventConfig(
    override val apiKey: String,
    override val baseUrl: String = "https://api.example.com/events/v1",
    override val timeout: Long = 30_000,
    val calendars: Set<String> = emptySet(),
    val filters: EventFilters = EventFilters(),
    val updateInterval: Duration = 30.seconds,
    val cacheExpiration: Duration = 5.minutes,
    val retryAttempts: Int = 3,
    val retryDelay: Duration = 1.seconds
) : ApiConfig {
    companion object {
        fun buildUrl(endpoint: String, params: Map<String, String>, apiKey: String): String {
            val baseUrl = "https://api.example.com/events/v1"
            val parameters = params.toMutableMap()
            parameters["apiKey"] = apiKey
            
            val queryString = parameters.entries.joinToString("&") { (key, value) ->
                "$key=$value"
            }
            
            return "$baseUrl/$endpoint?$queryString"
        }

        fun getEventsUrl(calendarId: String, apiKey: String): String =
            buildUrl("events", mapOf("calendarId" to calendarId), apiKey)

        fun getCalendarUrl(calendarId: String, apiKey: String): String =
            buildUrl("calendars", mapOf("calendarId" to calendarId), apiKey)
    }
}

/**
 * Filters for event queries.
 */
data class EventFilters(
    val categories: Set<String> = emptySet(),
    val locations: Set<String> = emptySet(),
    val excludeDeclined: Boolean = true,
    val excludeCancelled: Boolean = true
)
