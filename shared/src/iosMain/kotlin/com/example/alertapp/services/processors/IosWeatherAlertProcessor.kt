package com.example.alertapp.services.processors

import com.example.alertapp.models.*
import com.example.alertapp.network.services.WeatherApiService
import com.example.alertapp.services.base.*
import platform.Foundation.NSLog

class IosWeatherAlertProcessor(
    private val weatherApiService: WeatherApiService
) : WeatherAlertProcessor() {

    override suspend fun getCurrentWeather(latitude: Double, longitude: Double): WeatherData {
        return weatherApiService.getCurrentWeather(latitude, longitude)
    }

    override fun logWarning(message: String) {
        NSLog("⚠️ $message")
    }

    override fun logError(message: String, error: Throwable?) {
        if (error != null) {
            NSLog("❌ $message - ${error.message}")
        } else {
            NSLog("❌ $message")
        }
    }

    override fun logInfo(message: String) {
        NSLog("ℹ️ $message")
    }
}
