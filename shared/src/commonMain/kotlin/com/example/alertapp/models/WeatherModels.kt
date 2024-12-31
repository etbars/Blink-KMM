package com.example.alertapp.models

import kotlinx.serialization.Serializable
import kotlin.math.abs

@Serializable
data class WeatherData(
    val temperature: Double,
    val feelsLike: Double,
    val humidity: Int,
    val pressure: Int,
    val windSpeed: Double,
    val rainfall: Double = 0.0,
    val cloudiness: Int = 0,
    val uvIndex: Double = 0.0,
    val airQuality: Double = 0.0,
    val description: String,
    val timestamp: Long = 0
) {
    companion object {
        fun getMetricUnit(metric: WeatherMetric): String {
            return when (metric) {
                WeatherMetric.TEMPERATURE, WeatherMetric.FEELS_LIKE -> "°C"
                WeatherMetric.HUMIDITY, WeatherMetric.CLOUDINESS -> "%"
                WeatherMetric.PRESSURE -> "hPa"
                WeatherMetric.WIND_SPEED -> "m/s"
                WeatherMetric.RAINFALL -> "mm"
                WeatherMetric.UV_INDEX -> ""
                WeatherMetric.AIR_QUALITY -> "AQI"
            }
        }

        fun getValueForMetric(data: WeatherData, metric: WeatherMetric): Double {
            return when (metric) {
                WeatherMetric.TEMPERATURE -> data.temperature
                WeatherMetric.FEELS_LIKE -> data.feelsLike
                WeatherMetric.HUMIDITY -> data.humidity.toDouble()
                WeatherMetric.PRESSURE -> data.pressure.toDouble()
                WeatherMetric.WIND_SPEED -> data.windSpeed
                WeatherMetric.RAINFALL -> data.rainfall
                WeatherMetric.CLOUDINESS -> data.cloudiness.toDouble()
                WeatherMetric.UV_INDEX -> data.uvIndex
                WeatherMetric.AIR_QUALITY -> data.airQuality
            }
        }
    }
}

@Serializable
data class WeatherConditionRule(
    val metric: WeatherMetric,
    val operator: WeatherOperator,
    val value: Double,
    val unit: String = "",
    val duration: Int = 0 // Duration in hours, 0 means immediate
) {
    fun evaluate(weatherData: WeatherData): Boolean {
        val metricValue = WeatherData.getValueForMetric(weatherData, metric)

        return when (operator) {
            WeatherOperator.ABOVE -> metricValue > value
            WeatherOperator.BELOW -> metricValue < value
            WeatherOperator.EQUALS -> abs(metricValue - value) < 0.001 // Using small delta for floating point comparison
            WeatherOperator.NOT_EQUALS -> abs(metricValue - value) >= 0.001
        }
    }

    fun getDescription(): String {
        val metricName = metric.getDisplayName()
        val operatorStr = operator.getDisplayName()
        val valueStr = when (metric) {
            WeatherMetric.TEMPERATURE, WeatherMetric.FEELS_LIKE -> "$value°C"
            WeatherMetric.HUMIDITY, WeatherMetric.CLOUDINESS -> "$value%"
            WeatherMetric.PRESSURE -> "$value hPa"
            WeatherMetric.WIND_SPEED -> "$value m/s"
            WeatherMetric.RAINFALL -> "$value mm"
            WeatherMetric.UV_INDEX -> "$value"
            WeatherMetric.AIR_QUALITY -> "$value AQI"
        }

        val durationStr = if (duration > 0) {
            " for $duration ${if (duration == 1) "hour" else "hours"}"
        } else ""

        return "$metricName $operatorStr $valueStr$durationStr"
    }
}

@Serializable
enum class WeatherMetric {
    TEMPERATURE,
    FEELS_LIKE,
    HUMIDITY,
    PRESSURE,
    WIND_SPEED,
    RAINFALL,
    CLOUDINESS,
    UV_INDEX,
    AIR_QUALITY;

    fun getDisplayName(): String {
        return when (this) {
            TEMPERATURE -> "Temperature"
            FEELS_LIKE -> "Feels Like"
            HUMIDITY -> "Humidity"
            PRESSURE -> "Pressure"
            WIND_SPEED -> "Wind Speed"
            RAINFALL -> "Rainfall"
            CLOUDINESS -> "Cloudiness"
            UV_INDEX -> "UV Index"
            AIR_QUALITY -> "Air Quality"
        }
    }

    fun getDefaultThreshold(): Double {
        return when (this) {
            TEMPERATURE -> 25.0 // 25°C
            FEELS_LIKE -> 25.0 // 25°C
            HUMIDITY -> 70.0 // 70%
            PRESSURE -> 1013.0 // 1013 hPa (standard atmospheric pressure)
            WIND_SPEED -> 10.0 // 10 m/s
            RAINFALL -> 5.0 // 5mm
            CLOUDINESS -> 75.0 // 75%
            UV_INDEX -> 6.0 // 6 UV index
            AIR_QUALITY -> 100.0 // 100 AQI
        }
    }

    fun getValueRange(): ClosedRange<Double> {
        return when (this) {
            TEMPERATURE, FEELS_LIKE -> -50.0..60.0
            HUMIDITY, CLOUDINESS -> 0.0..100.0
            PRESSURE -> 870.0..1085.0
            WIND_SPEED -> 0.0..200.0
            RAINFALL -> 0.0..500.0
            UV_INDEX -> 0.0..12.0
            AIR_QUALITY -> 0.0..500.0
        }
    }
}

@Serializable
enum class WeatherOperator {
    ABOVE,
    BELOW,
    EQUALS,
    NOT_EQUALS;

    fun getDisplayName(): String {
        return when (this) {
            ABOVE -> "is above"
            BELOW -> "is below"
            EQUALS -> "equals"
            NOT_EQUALS -> "is not"
        }
    }
}
