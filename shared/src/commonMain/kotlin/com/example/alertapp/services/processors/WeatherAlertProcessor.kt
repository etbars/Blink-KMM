package com.example.alertapp.services.processors

import com.example.alertapp.models.Alert
import com.example.alertapp.models.ProcessingResult
import com.example.alertapp.models.AlertTrigger
import com.example.alertapp.enums.Operator
import com.example.alertapp.models.weather.WeatherCondition
import com.example.alertapp.models.weather.WeatherConditionType
import com.example.alertapp.models.weather.WeatherLocation

abstract class WeatherAlertProcessor : BaseAlertProcessor() {
    abstract suspend fun getCurrentWeather(latitude: Double, longitude: Double): WeatherResult
    abstract fun logWarning(message: String)
    abstract fun logError(message: String, error: Throwable? = null)
    abstract fun logInfo(message: String)

    override suspend fun processAlert(alert: Alert): ProcessingResult {
        if (!validateAlert(alert)) {
            return ProcessingResult.Error("Invalid alert configuration", "VALIDATION_ERROR")
        }

        return try {
            val trigger = alert.trigger as? AlertTrigger.WeatherTrigger
                ?: return ProcessingResult.Error("Invalid trigger type", "INVALID_TRIGGER")

            when (val result = getCurrentWeather(trigger.location.latitude, trigger.location.longitude)) {
                is WeatherResult.Success -> {
                    val triggeredConditions = trigger.conditions.filter { condition ->
                        checkCondition(condition, result.data)
                    }

                    if (triggeredConditions.isEmpty()) {
                        ProcessingResult.NotTriggered("Weather conditions not met")
                    } else {
                        ProcessingResult.Triggered(
                            "Weather conditions met",
                            mapOf(
                                "location" to "${trigger.location.latitude},${trigger.location.longitude}",
                                "conditions" to triggeredConditions.joinToString { it.type.toString() }
                            )
                        )
                    }
                }
                is WeatherResult.Error -> {
                    ProcessingResult.Error(result.message, "WEATHER_ERROR")
                }
            }
        } catch (e: Exception) {
            logError("Error processing weather alert", e)
            ProcessingResult.Error(e.message ?: "Unknown error", "PROCESSING_ERROR")
        }
    }

    private fun checkCondition(condition: WeatherCondition, weather: WeatherData): Boolean {
        val currentValue = when (condition.type) {
            WeatherConditionType.TEMPERATURE -> weather.temperature
            WeatherConditionType.HUMIDITY -> weather.humidity
            WeatherConditionType.WIND_SPEED -> weather.windSpeed
            WeatherConditionType.PRECIPITATION -> weather.precipitation
            WeatherConditionType.PRESSURE -> weather.pressure
            WeatherConditionType.CLOUDINESS -> weather.cloudiness?.toDouble()
            WeatherConditionType.UV_INDEX -> weather.metadata["uv_index"]?.toDoubleOrNull()
            WeatherConditionType.AIR_QUALITY -> weather.metadata["air_quality"]?.toDoubleOrNull()
            WeatherConditionType.CUSTOM -> condition.customValue?.toDoubleOrNull()
        } ?: return false

        return condition.threshold?.let { threshold ->
            when (condition.operator) {
                Operator.GREATER_THAN -> currentValue > threshold
                Operator.LESS_THAN -> currentValue < threshold
                Operator.EQUAL_TO -> currentValue.compareTo(threshold) == 0
                Operator.GREATER_THAN_OR_EQUAL -> currentValue >= threshold
                Operator.LESS_THAN_OR_EQUAL -> currentValue <= threshold
                Operator.NOT_EQUAL_TO -> currentValue.compareTo(threshold) != 0
            }
        } ?: false
    }
}

sealed class WeatherResult {
    data class Success(val data: WeatherData) : WeatherResult()
    data class Error(val message: String, val cause: Throwable? = null) : WeatherResult()
}

data class WeatherData(
    val temperature: Double,
    val humidity: Double,
    val windSpeed: Double,
    val precipitation: Double,
    val pressure: Double,
    val cloudiness: Int?,
    val metadata: Map<String, String> = emptyMap()
)
