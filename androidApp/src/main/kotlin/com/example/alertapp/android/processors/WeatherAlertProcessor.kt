package com.example.alertapp.android.processors

import android.location.Location
import com.example.alertapp.android.location.LocationProvider
import com.example.alertapp.models.Alert
import com.example.alertapp.models.AlertType
import com.example.alertapp.models.WeatherCondition
import com.example.alertapp.models.WeatherData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToInt

@Singleton
class WeatherAlertProcessor @Inject constructor(
    private val locationProvider: LocationProvider,
    private val weatherApi: WeatherApi
) {
    suspend fun processAlert(alert: Alert): Boolean {
        if (alert.type != AlertType.WEATHER) return false

        val location = locationProvider.getCurrentLocation() ?: return false
        val weatherData = fetchWeatherData(location)

        return evaluateWeatherConditions(alert, weatherData)
    }

    private suspend fun fetchWeatherData(location: Location): WeatherData {
        return weatherApi.getCurrentWeather(
            latitude = location.latitude,
            longitude = location.longitude
        )
    }

    private fun evaluateWeatherConditions(alert: Alert, weatherData: WeatherData): Boolean {
        val conditions = alert.conditions.mapNotNull { 
            WeatherCondition.fromString(it)
        }

        return conditions.any { condition ->
            when (condition) {
                WeatherCondition.TEMPERATURE_ABOVE -> {
                    val threshold = alert.threshold?.toDoubleOrNull() ?: return false
                    weatherData.temperature > threshold
                }
                WeatherCondition.TEMPERATURE_BELOW -> {
                    val threshold = alert.threshold?.toDoubleOrNull() ?: return false
                    weatherData.temperature < threshold
                }
                WeatherCondition.RAIN -> {
                    weatherData.precipitation > 0.1
                }
                WeatherCondition.SNOW -> {
                    weatherData.snowfall > 0.1
                }
                WeatherCondition.HIGH_WIND -> {
                    weatherData.windSpeed > 30.0
                }
                WeatherCondition.STORM -> {
                    weatherData.precipitation > 10.0 || weatherData.windSpeed > 50.0
                }
            }
        }
    }

    fun formatWeatherMessage(alert: Alert, weatherData: WeatherData): String {
        val conditions = alert.conditions.mapNotNull { WeatherCondition.fromString(it) }
        val triggeredConditions = conditions.filter { condition ->
            when (condition) {
                WeatherCondition.TEMPERATURE_ABOVE -> {
                    val threshold = alert.threshold?.toDoubleOrNull() ?: return@filter false
                    weatherData.temperature > threshold
                }
                WeatherCondition.TEMPERATURE_BELOW -> {
                    val threshold = alert.threshold?.toDoubleOrNull() ?: return@filter false
                    weatherData.temperature < threshold
                }
                WeatherCondition.RAIN -> weatherData.precipitation > 0.1
                WeatherCondition.SNOW -> weatherData.snowfall > 0.1
                WeatherCondition.HIGH_WIND -> weatherData.windSpeed > 30.0
                WeatherCondition.STORM -> {
                    weatherData.precipitation > 10.0 || weatherData.windSpeed > 50.0
                }
            }
        }

        return buildString {
            append(alert.name)
            append("\n\nCurrent conditions:\n")
            append("Temperature: ${weatherData.temperature.roundToInt()}°C\n")
            append("Wind Speed: ${weatherData.windSpeed.roundToInt()} km/h\n")
            if (weatherData.precipitation > 0) {
                append("Precipitation: ${weatherData.precipitation} mm\n")
            }
            if (weatherData.snowfall > 0) {
                append("Snowfall: ${weatherData.snowfall} mm\n")
            }
            append("\nTriggered conditions:\n")
            triggeredConditions.forEach { condition ->
                append("- $condition\n")
            }
        }
    }
}
