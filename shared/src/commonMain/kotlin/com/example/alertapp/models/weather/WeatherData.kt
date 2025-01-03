package com.example.alertapp.models.weather

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

sealed class WeatherData {
    @Serializable
    data class Current(
        val temperature: Double,
        val feelsLike: Double,
        val humidity: Int,
        val pressure: Int,
        val windSpeed: Double,
        val windDirection: Int,
        val description: String,
        val cloudCover: Int?,
        val precipitation: Double?,
        val timestamp: Instant
    )

    @Serializable
    data class Forecast(
        val hourly: List<HourlyForecast>,
        val daily: List<DailyForecast>
    )
}

@Serializable
data class HourlyForecast(
    val timestamp: Instant,
    val temperature: Double,
    val feelsLike: Double,
    val humidity: Int,
    val pressure: Int,
    val windSpeed: Double,
    val windDirection: Int,
    val description: String,
    val cloudCover: Int?,
    val precipitation: Double?,
    val icon: String
)

@Serializable
data class DailyForecast(
    val timestamp: Instant,
    val tempMin: Double,
    val tempMax: Double,
    val humidity: Int,
    val pressure: Int,
    val windSpeed: Double,
    val windDirection: Int,
    val description: String,
    val icon: String,
    val cloudCover: Int,
    val precipitation: Double
)
