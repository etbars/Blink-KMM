package com.example.alertapp.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class WeatherCondition {
    @SerialName("temperature")
    TEMPERATURE,
    @SerialName("humidity")
    HUMIDITY,
    @SerialName("wind_speed")
    WIND_SPEED,
    @SerialName("precipitation")
    PRECIPITATION,
    @SerialName("cloud_cover")
    CLOUD_COVER,
    @SerialName("uv_index")
    UV_INDEX;

    companion object {
        fun fromString(value: String): WeatherCondition {
            return try {
                valueOf(value.uppercase())
            } catch (e: IllegalArgumentException) {
                TEMPERATURE // Default to TEMPERATURE if unknown condition
            }
        }

        fun toString(condition: WeatherCondition): String {
            return when (condition) {
                TEMPERATURE -> "temperature"
                HUMIDITY -> "humidity"
                WIND_SPEED -> "wind_speed"
                PRECIPITATION -> "precipitation"
                CLOUD_COVER -> "cloud_cover"
                UV_INDEX -> "uv_index"
            }
        }
    }
}
