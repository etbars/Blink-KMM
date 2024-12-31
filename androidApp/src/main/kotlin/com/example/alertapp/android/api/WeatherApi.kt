package com.example.alertapp.android.api

import com.example.alertapp.android.cache.CacheManager
import com.example.alertapp.android.processors.WeatherData
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query
import javax.inject.Inject
import javax.inject.Singleton

interface WeatherApi {
    @GET("data/2.5/weather")
    suspend fun getCurrentWeather(
        @Query("lat") latitude: Double,
        @Query("lon") longitude: Double,
        @Query("units") units: String = "metric",
        @Query("appid") apiKey: String
    ): Response<OpenWeatherResponse>
}

data class OpenWeatherResponse(
    val main: Main,
    val wind: Wind,
    val rain: Rain?,
    val snow: Snow?,
    val weather: List<Weather>
) {
    data class Main(
        val temp: Double,
        val humidity: Double,
        val pressure: Double
    )

    data class Wind(
        val speed: Double
    )

    data class Rain(
        val `1h`: Double = 0.0
    )

    data class Snow(
        val `1h`: Double = 0.0
    )

    data class Weather(
        val description: String,
        val icon: String
    )
}

@Singleton
class WeatherApiImpl @Inject constructor(
    private val api: WeatherApi,
    private val apiKey: String,
    private val cacheManager: CacheManager
) {
    companion object {
        private const val MAX_RETRIES = 3
        private const val RETRY_DELAY_MS = 1000L
    }

    suspend fun getCurrentWeather(latitude: Double, longitude: Double): ApiResult<WeatherData> {
        val locationKey = "$latitude,$longitude"
        
        // Check cache first
        cacheManager.getWeatherData(locationKey)?.let { cachedData ->
            return ApiResult.Success(cachedData)
        }

        var lastException: Exception? = null
        repeat(MAX_RETRIES) { attempt ->
            try {
                val response = api.getCurrentWeather(
                    latitude = latitude,
                    longitude = longitude,
                    units = "metric",
                    apiKey = this.apiKey
                )

                if (!response.isSuccessful) {
                    val errorBody = response.errorBody()?.string()
                    if (errorBody?.contains("Invalid API key") == true) {
                        return ApiResult.Error(
                            ErrorCode.API_ERROR,
                            "Invalid OpenWeather API key"
                        )
                    }
                    
                    return ApiResult.Error(
                        ErrorCode.API_ERROR,
                        "API error: ${response.code()} - ${response.message()}"
                    )
                }

                val weatherResponse = response.body()
                if (weatherResponse == null) {
                    return ApiResult.Error(
                        ErrorCode.PARSE_ERROR,
                        "Invalid response format"
                    )
                }

                val weatherData = mapToWeatherData(weatherResponse)
                // Cache the successful response
                cacheManager.cacheWeatherData(locationKey, weatherData)
                return ApiResult.Success(weatherData)

            } catch (e: Exception) {
                lastException = e
                if (attempt < MAX_RETRIES - 1) {
                    kotlinx.coroutines.delay(RETRY_DELAY_MS)
                }
            }
        }

        return ApiResult.Error(
            ErrorCode.NETWORK_ERROR,
            "Network error after $MAX_RETRIES attempts: ${lastException?.message}"
        )
    }

    private fun mapToWeatherData(response: OpenWeatherResponse): WeatherData {
        return WeatherData(
            temperature = response.main.temp,
            windSpeed = response.wind.speed,
            precipitation = response.rain?.`1h` ?: 0.0,
            snowfall = response.snow?.`1h` ?: 0.0,
            humidity = response.main.humidity,
            pressure = response.main.pressure,
            description = response.weather.firstOrNull()?.description ?: ""
        )
    }
}
