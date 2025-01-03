package com.example.alertapp.api.weather

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
data class WeatherData(
    val temperature: Double,
    val humidity: Double,
    val windSpeed: Double,
    val precipitation: Double,
    val uvIndex: Double,
    val airQuality: Double,
    val timestamp: Instant
)
