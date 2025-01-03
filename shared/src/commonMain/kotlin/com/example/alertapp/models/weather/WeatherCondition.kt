package com.example.alertapp.models.weather

import com.example.alertapp.enums.Operator
import kotlinx.serialization.Serializable

@Serializable
data class WeatherCondition(
    val type: WeatherConditionType,
    val operator: Operator = Operator.GREATER_THAN,
    val threshold: Double? = null,
    val customValue: String? = null
)

@Serializable
enum class WeatherConditionType {
    TEMPERATURE,
    HUMIDITY,
    WIND_SPEED,
    PRECIPITATION,
    PRESSURE,
    CLOUDINESS,
    UV_INDEX,
    AIR_QUALITY,
    CUSTOM;

    companion object {
        fun fromValue(value: String): WeatherConditionType = valueOf(value.uppercase())
    }

    override fun toString(): String = name.lowercase()
}
