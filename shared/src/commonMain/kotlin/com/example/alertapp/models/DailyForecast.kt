package com.example.alertapp.models

import kotlinx.serialization.Serializable

@Serializable
data class DailyForecast(
    val timestamp: Long,
    val tempMin: Double,
    val tempMax: Double,
    val humidity: Int,
    val pressure: Int,
    val windSpeed: Double,
    val windDirection: Int,
    val description: String,
    val icon: String,
    val cloudCover: Int,
    val precipitation: Double = 0.0,
    val uvIndex: Double = 0.0
)
