package com.example.alertapp.services.processors

import com.example.alertapp.models.*
import com.example.alertapp.services.base.*
import kotlinx.serialization.Serializable
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

abstract class WeatherAlertProcessor : BaseAlertProcessor() {
    override val supportedType: AlertType = AlertType.WEATHER

    override suspend fun processAlert(alert: Alert): ProcessingResult {
        if (alert.trigger !is AlertTrigger.WeatherTrigger) {
            return error("Invalid trigger type for weather alert")
        }

        val trigger = alert.trigger as AlertTrigger.WeatherTrigger
        val validationResult = validateAlertSpecific(trigger)
        if (!validationResult.first) {
            return notTriggered(validationResult.second)
        }

        return try {
            // Get current weather data
            val weatherData = getCurrentWeather(trigger.latitude, trigger.longitude)
            val triggered = evaluateConditions(trigger.conditions, weatherData)
            
            if (triggered) {
                triggered(
                    message = "Weather conditions match alert criteria",
                    data = mapOf(
                        "temperature" to weatherData.temperature.toString(),
                        "humidity" to weatherData.humidity.toString(),
                        "description" to weatherData.description,
                        "pressure" to weatherData.pressure.toString(),
                        "windSpeed" to weatherData.windSpeed.toString(),
                        "cloudiness" to weatherData.cloudiness.toString()
                    ),
                    metadata = mapOf(
                        "location" to "${trigger.latitude},${trigger.longitude}",
                        "timestamp" to weatherData.timestamp.toString()
                    )
                )
            } else {
                notTriggered("Weather conditions do not match alert criteria")
            }
        } catch (e: Exception) {
            logError("Error processing weather alert", e)
            error("Failed to process weather alert: ${e.message}")
        }
    }

    override fun getConfigurationSchema(): Map<String, ConfigurationField> = mapOf(
        "latitude" to ConfigurationField(
            type = ConfigurationFieldType.NUMBER,
            required = true,
            description = "Latitude of the location to monitor"
        ),
        "longitude" to ConfigurationField(
            type = ConfigurationFieldType.NUMBER,
            required = true,
            description = "Longitude of the location to monitor"
        ),
        "conditions" to ConfigurationField(
            type = ConfigurationFieldType.OBJECT,
            required = true,
            description = "Weather conditions to monitor",
            options = listOf(
                "temperature", "humidity", "pressure",
                "windSpeed", "cloudiness", "precipitation"
            )
        )
    )

    protected fun validateAlertSpecific(trigger: AlertTrigger.WeatherTrigger): Pair<Boolean, String> {
        if (trigger.latitude < -90 || trigger.latitude > 90) {
            logWarning("Invalid latitude: ${trigger.latitude}")
            return false to "Latitude must be between -90 and 90"
        }

        if (trigger.longitude < -180 || trigger.longitude > 180) {
            logWarning("Invalid longitude: ${trigger.longitude}")
            return false to "Longitude must be between -180 and 180"
        }

        if (trigger.conditions.isEmpty()) {
            logWarning("No weather conditions specified")
            return false to "At least one weather condition must be specified"
        }

        return true to ""
    }

    protected abstract suspend fun getCurrentWeather(latitude: Double, longitude: Double): WeatherData

    protected fun evaluateConditions(conditions: List<WeatherCondition>, weatherData: WeatherData): Boolean {
        return conditions.all { condition ->
            when (condition) {
                is WeatherCondition.Temperature -> evaluateValue(
                    condition.operator,
                    weatherData.temperature,
                    condition.value
                )
                is WeatherCondition.Humidity -> evaluateValue(
                    condition.operator,
                    weatherData.humidity,
                    condition.value
                )
                is WeatherCondition.WindSpeed -> evaluateValue(
                    condition.operator,
                    weatherData.windSpeed,
                    condition.value
                )
                is WeatherCondition.Cloudiness -> evaluateValue(
                    condition.operator,
                    weatherData.cloudiness,
                    condition.value
                )
                is WeatherCondition.Precipitation -> weatherData.precipitation >= condition.value
            }
        }
    }

    private fun evaluateValue(operator: Operator, actual: Double, expected: Double): Boolean {
        return when (operator) {
            Operator.GREATER_THAN -> actual > expected
            Operator.LESS_THAN -> actual < expected
            Operator.EQUALS -> actual == expected
        }
    }
}

@Serializable
data class WeatherData(
    val temperature: Double,
    val humidity: Double,
    val pressure: Double,
    val windSpeed: Double,
    val cloudiness: Double,
    val precipitation: Double,
    val description: String,
    val timestamp: Long
)
