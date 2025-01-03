package com.example.alertapp.models

import com.example.alertapp.models.weather.WeatherLocation
import kotlinx.serialization.Serializable
import kotlinx.datetime.Instant

@Serializable
data class WeatherData(
    val location: WeatherLocation,
    val temperature: Double,
    val feelsLike: Double,
    val humidity: Int,
    val pressure: Int,
    val windSpeed: Double,
    val description: String,
    val icon: String,
    val timestamp: Instant,
    val forecast: List<ForecastItem>
)

@Serializable
data class ForecastItem(
    val timestamp: Instant,
    val temperature: Double,
    val feelsLike: Double,
    val humidity: Int,
    val pressure: Int,
    val windSpeed: Double,
    val description: String,
    val icon: String
)
