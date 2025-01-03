package com.example.alertapp.models

import kotlinx.serialization.Serializable

@Serializable
data class WeatherConditions(
    val temperature: Double,
    val feelsLike: Double,
    val humidity: Int,
    val pressure: Int,
    val windSpeed: Double,
    val windDirection: Int,
    val description: String,
    val icon: String,
    val cloudCover: Int,
    val visibility: Int,
    val precipitation: Double = 0.0,
    val uvIndex: Double = 0.0,
    val timestamp: Long
)
