package com.example.alertapp.services.processors

import com.example.alertapp.network.services.WeatherApiService
import com.example.alertapp.services.base.NotificationHandler
import com.example.alertapp.services.processors.WeatherData
import com.example.alertapp.services.processors.WeatherResult
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.datetime.Clock

@Singleton
class AndroidWeatherAlertProcessor @Inject constructor(
    private val weatherApiService: WeatherApiService,
    private val notificationHandler: NotificationHandler
) : WeatherAlertProcessor() {

    override suspend fun getCurrentWeather(latitude: Double, longitude: Double): WeatherResult {
        return try {
            val response = weatherApiService.getCurrentWeather(latitude, longitude)
            val weatherData = WeatherData(
                temperature = response.temperature,
                humidity = response.humidity.toDouble(),
                windSpeed = response.windSpeed,
                precipitation = response.precipitation ?: 0.0,
                pressure = response.pressure.toDouble(),
                cloudiness = response.cloudCover,
                metadata = mapOf(
                    "description" to response.description,
                    "windDirection" to response.windDirection.toString(),
                    "feelsLike" to response.feelsLike.toString()
                )
            )
            WeatherResult.Success(weatherData)
        } catch (e: Exception) {
            logError("Failed to fetch weather data", e)
            WeatherResult.Error(e.message ?: "Unknown error", e)
        }
    }

    override fun logWarning(message: String) {
        Timber.w(message)
    }

    override fun logError(message: String, error: Throwable?) {
        if (error != null) {
            Timber.e(error, message)
        } else {
            Timber.e(message)
        }
    }

    override fun logInfo(message: String) {
        Timber.i(message)
    }
}
