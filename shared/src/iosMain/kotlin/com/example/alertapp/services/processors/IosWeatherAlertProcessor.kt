package com.example.alertapp.services.processors

import com.example.alertapp.models.Alert
import com.example.alertapp.models.AlertTrigger
import com.example.alertapp.models.WeatherData
import com.example.alertapp.api.weather.WeatherProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import platform.Foundation.NSLog

class IosWeatherAlertProcessor(
    private val weatherProvider: WeatherProvider
) : WeatherAlertProcessor() {
    
    override suspend fun getCurrentWeather(latitude: Double, longitude: Double): WeatherData {
        return weatherProvider.getCurrentWeather(latitude, longitude)
    }

    override fun validateAlertSpecific(alert: Alert): Boolean {
        if (alert.trigger !is AlertTrigger.WeatherTrigger) {
            NSLog("Invalid trigger type for weather alert")
            return false
        }
        
        val trigger = alert.trigger
        return trigger.location.latitude in -90.0..90.0 &&
                trigger.location.longitude in -180.0..180.0 &&
                trigger.threshold >= 0
    }

    override suspend fun processAlert(alert: Alert): ProcessingResult {
        try {
            if (!validateAlertSpecific(alert)) {
                return ProcessingResult.Invalid("Invalid weather alert configuration")
            }

            val trigger = alert.trigger as AlertTrigger.WeatherTrigger
            val weather = getCurrentWeather(trigger.location.latitude, trigger.location.longitude)
            
            val shouldTrigger = when (trigger.condition) {
                WeatherCondition.TEMPERATURE -> weather.current.temperature > trigger.threshold
                WeatherCondition.HUMIDITY -> weather.current.humidity > trigger.threshold
                WeatherCondition.WIND_SPEED -> weather.current.windSpeed > trigger.threshold
                WeatherCondition.PRECIPITATION -> weather.current.precipitation > trigger.threshold
                WeatherCondition.CLOUD_COVER -> weather.current.cloudCover > trigger.threshold
                WeatherCondition.UV_INDEX -> weather.current.uvIndex > trigger.threshold
            }

            return if (shouldTrigger) {
                ProcessingResult.Triggered("Weather condition met: ${trigger.condition}")
            } else {
                ProcessingResult.NotTriggered
            }
        } catch (e: Exception) {
            NSLog("Failed to process weather alert: ${e.message}")
            return ProcessingResult.Error(e)
        }
    }
}
