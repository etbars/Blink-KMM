package com.example.alertapp.models

import kotlinx.serialization.Serializable

@Serializable
data class WeatherConditionRule(
    val metric: WeatherMetric,
    val operator: WeatherOperator,
    val value: Double,
    val unit: String = "",
    val duration: Int = 0 // Duration in hours, 0 means immediate
) {
    fun evaluate(metricValue: Double): Boolean {
        return when (operator) {
            WeatherOperator.ABOVE -> metricValue > value
            WeatherOperator.BELOW -> metricValue < value
            WeatherOperator.EQUALS -> kotlin.math.abs(metricValue - value) < 0.001 // Using small delta for floating point comparison
            WeatherOperator.NOT_EQUALS -> kotlin.math.abs(metricValue - value) >= 0.001
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
