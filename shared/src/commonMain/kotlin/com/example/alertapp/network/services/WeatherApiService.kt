package com.example.alertapp.network.services

import com.example.alertapp.models.weather.WeatherData
import com.example.alertapp.models.weather.HourlyForecast
import com.example.alertapp.models.weather.DailyForecast
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.json.*
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class WeatherApiService : KoinComponent {
    private val httpClient: HttpClient by inject()
    private val apiKey: String by inject()
    private val baseUrl: String = "https://api.openweathermap.org/data/2.5"

    suspend fun getCurrentWeather(lat: Double, lon: Double): WeatherData.Current {
        try {
            val response = httpClient.get("$baseUrl/weather") {
                parameter("lat", lat)
                parameter("lon", lon)
                parameter("appid", apiKey)
                parameter("units", "metric")
            }

            val json = response.body<JsonObject>()
            val main = json["main"]?.jsonObject ?: throw IllegalStateException("Invalid weather response")
            val weather = json["weather"]?.jsonArray ?: throw IllegalStateException("Invalid weather response")
            val wind = json["wind"]?.jsonObject ?: throw IllegalStateException("Invalid weather response")
            val clouds = json["clouds"]?.jsonObject ?: throw IllegalStateException("Invalid weather response")
            val rain = json["rain"]?.jsonObject

            return WeatherData.Current(
                temperature = main["temp"]?.jsonPrimitive?.double ?: 0.0,
                feelsLike = main["feels_like"]?.jsonPrimitive?.double ?: 0.0,
                humidity = main["humidity"]?.jsonPrimitive?.int ?: 0,
                pressure = main["pressure"]?.jsonPrimitive?.int ?: 0,
                windSpeed = wind["speed"]?.jsonPrimitive?.double ?: 0.0,
                windDirection = wind["deg"]?.jsonPrimitive?.int ?: 0,
                description = weather.firstOrNull()?.jsonObject?.get("description")?.jsonPrimitive?.content ?: "",
                cloudCover = clouds["all"]?.jsonPrimitive?.int,
                precipitation = rain?.get("1h")?.jsonPrimitive?.double,
                timestamp = Clock.System.now()
            )
        } catch (e: Exception) {
            throw IllegalStateException("Failed to get current weather", e)
        }
    }

    suspend fun getForecast(lat: Double, lon: Double, days: Int = 5): WeatherData.Forecast {
        try {
            val response = httpClient.get("$baseUrl/forecast") {
                parameter("lat", lat)
                parameter("lon", lon)
                parameter("appid", apiKey)
                parameter("units", "metric")
                parameter("cnt", days * 8) // API returns data in 3-hour intervals, so 8 points per day
            }

            val json = response.body<JsonObject>()
            val list = json["list"]?.jsonArray ?: throw IllegalStateException("Invalid forecast response")

            val hourlyForecasts = list.map { itemJson ->
                try {
                    val itemObj = itemJson.jsonObject
                    val main = itemObj["main"]?.jsonObject ?: throw IllegalStateException("Invalid forecast item")
                    val weather = itemObj["weather"]?.jsonArray ?: throw IllegalStateException("Invalid forecast item")
                    val wind = itemObj["wind"]?.jsonObject ?: throw IllegalStateException("Invalid forecast item")
                    val clouds = itemObj["clouds"]?.jsonObject
                    val rain = itemObj["rain"]?.jsonObject

                    HourlyForecast(
                        timestamp = Instant.fromEpochSeconds(itemObj["dt"]?.jsonPrimitive?.long ?: 0L),
                        temperature = main["temp"]?.jsonPrimitive?.double ?: 0.0,
                        feelsLike = main["feels_like"]?.jsonPrimitive?.double ?: 0.0,
                        humidity = main["humidity"]?.jsonPrimitive?.int ?: 0,
                        pressure = main["pressure"]?.jsonPrimitive?.int ?: 0,
                        windSpeed = wind["speed"]?.jsonPrimitive?.double ?: 0.0,
                        windDirection = wind["deg"]?.jsonPrimitive?.int ?: 0,
                        description = weather.firstOrNull()?.jsonObject?.get("description")?.jsonPrimitive?.content ?: "",
                        cloudCover = clouds?.get("all")?.jsonPrimitive?.int,
                        precipitation = rain?.get("3h")?.jsonPrimitive?.double,
                        icon = weather.firstOrNull()?.jsonObject?.get("icon")?.jsonPrimitive?.content ?: ""
                    )
                } catch (e: Exception) {
                    throw IllegalStateException("Failed to parse forecast item", e)
                }
            }

            // Group forecasts by day and create daily forecasts
            val dailyForecasts = hourlyForecasts
                .groupBy { it.timestamp.epochSeconds / (24 * 3600) }
                .map { (_, dayForecasts) ->
                    try {
                        DailyForecast(
                            timestamp = dayForecasts.first().timestamp,
                            tempMin = dayForecasts.minOf { it.temperature },
                            tempMax = dayForecasts.maxOf { it.temperature },
                            humidity = dayForecasts.map { it.humidity }.average().toInt(),
                            pressure = dayForecasts.map { it.pressure }.average().toInt(),
                            windSpeed = dayForecasts.map { it.windSpeed }.average(),
                            windDirection = dayForecasts.map { it.windDirection }.average().toInt(),
                            description = dayForecasts.first().description,
                            icon = dayForecasts.first().icon,
                            cloudCover = dayForecasts.mapNotNull { it.cloudCover }.average().toInt(),
                            precipitation = dayForecasts.mapNotNull { it.precipitation }.average()
                        )
                    } catch (e: Exception) {
                        throw IllegalStateException("Failed to create daily forecast", e)
                    }
                }

            return WeatherData.Forecast(
                hourly = hourlyForecasts,
                daily = dailyForecasts
            )
        } catch (e: Exception) {
            throw IllegalStateException("Failed to get forecast", e)
        }
    }
}
