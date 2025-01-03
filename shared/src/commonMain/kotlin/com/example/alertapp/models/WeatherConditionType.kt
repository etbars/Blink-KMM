package com.example.alertapp.models

import kotlinx.serialization.Serializable

@Serializable
enum class WeatherConditionType {
    TEMPERATURE,
    HUMIDITY,
    WIND_SPEED,
    PRECIPITATION,
    UV_INDEX,
    AIR_QUALITY
}
