package com.example.alertapp.models.weather

import kotlinx.serialization.Serializable

@Serializable
data class WeatherConditionRule(
    val type: WeatherConditionType,
    val operator: String,
    val value: Double,
    val unit: String? = null
)
