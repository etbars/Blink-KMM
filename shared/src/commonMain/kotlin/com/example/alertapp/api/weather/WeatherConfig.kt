package com.example.alertapp.api.weather

import com.example.alertapp.api.ApiConfig
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/**
 * Configuration for weather-related API endpoints and settings.
 */
data class WeatherConfig(
    override val apiKey: String,
    override val baseUrl: String = "https://api.openweathermap.org/data/3.0",
    override val timeout: Long = 30_000,
    val units: String = "metric",
    val language: String = "en",
    val updateInterval: Duration = 30.seconds,
    val cacheExpiration: Duration = 5.minutes,
    val retryAttempts: Int = 3,
    val retryDelay: Duration = 1.seconds
) : ApiConfig {
    companion object {
        fun buildUrl(endpoint: String, params: Map<String, String>, apiKey: String): String {
            val baseUrl = "https://api.openweathermap.org/data/3.0"
            val parameters = params.toMutableMap()
            parameters["appid"] = apiKey
            
            val queryString = parameters.entries.joinToString("&") { (key, value) ->
                "$key=$value"
            }
            
            return "$baseUrl/$endpoint?$queryString"
        }

        fun getCurrentWeatherUrl(latitude: Double, longitude: Double, units: String, apiKey: String): String =
            buildUrl(
                "weather",
                mapOf(
                    "lat" to latitude.toString(),
                    "lon" to longitude.toString(),
                    "units" to units
                ),
                apiKey
            )

        fun getForecastUrl(latitude: Double, longitude: Double, units: String, apiKey: String): String =
            buildUrl(
                "onecall",
                mapOf(
                    "lat" to latitude.toString(),
                    "lon" to longitude.toString(),
                    "units" to units,
                    "exclude" to "minutely,current"
                ),
                apiKey
            )

        fun getAlertsUrl(latitude: Double, longitude: Double, apiKey: String): String =
            buildUrl(
                "onecall",
                mapOf(
                    "lat" to latitude.toString(),
                    "lon" to longitude.toString(),
                    "exclude" to "current,minutely,hourly,daily"
                ),
                apiKey
            )
    }
}
