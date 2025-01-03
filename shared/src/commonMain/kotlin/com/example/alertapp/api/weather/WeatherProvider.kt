package com.example.alertapp.api.weather

import com.example.alertapp.api.ApiResponse
import com.example.alertapp.models.weather.WeatherData
import kotlinx.datetime.Instant
import co.touchlab.kermit.Logger

interface WeatherProvider {
    suspend fun getCurrentWeather(latitude: Double, longitude: Double): ApiResponse<WeatherData.Current>
}

class DefaultWeatherProvider(
    private val logger: Logger = Logger.withTag("WeatherProvider")
) : WeatherProvider {
    override suspend fun getCurrentWeather(latitude: Double, longitude: Double): ApiResponse<WeatherData.Current> {
        // TODO: Implement actual weather fetching logic
        return ApiResponse.Success(WeatherData.Current(
            temperature = 0.0,
            feelsLike = 0.0,
            humidity = 0,
            pressure = 0,
            windSpeed = 0.0,
            windDirection = 0,
            description = "Unknown",
            cloudCover = 0,
            precipitation = 0.0,
            timestamp = Instant.fromEpochSeconds(0)
        ))
    }
}
