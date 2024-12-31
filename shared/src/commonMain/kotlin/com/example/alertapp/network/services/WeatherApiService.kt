package com.example.alertapp.network.services

import com.example.alertapp.models.WeatherData
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.datetime.Clock
import kotlinx.serialization.json.*
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class WeatherApiService : KoinComponent {
    private val httpClient: HttpClient by inject()
    private val apiKey: String by inject()
    private val baseUrl: String = "https://api.openweathermap.org/data/2.5"

    suspend fun getCurrentWeather(lat: Double, lon: Double): WeatherData {
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

        return WeatherData(
            temperature = main["temp"]?.jsonPrimitive?.double ?: 0.0,
            humidity = main["humidity"]?.jsonPrimitive?.double?.toDouble() ?: 0.0,
            pressure = main["pressure"]?.jsonPrimitive?.double?.toDouble() ?: 0.0,
            windSpeed = wind["speed"]?.jsonPrimitive?.double ?: 0.0,
            cloudiness = clouds["all"]?.jsonPrimitive?.double?.toDouble() ?: 0.0,
            precipitation = rain?.get("1h")?.jsonPrimitive?.double ?: 0.0,
            description = weather.firstOrNull()?.jsonObject?.get("description")?.jsonPrimitive?.content ?: "",
            timestamp = Clock.System.now().epochSeconds
        )
    }

    suspend fun getForecast(lat: Double, lon: Double, days: Int = 5): List<WeatherData> {
        val response = httpClient.get("$baseUrl/forecast") {
            parameter("lat", lat)
            parameter("lon", lon)
            parameter("appid", apiKey)
            parameter("units", "metric")
            parameter("cnt", days * 8) // API returns data in 3-hour intervals
        }

        val json = response.body<JsonObject>()
        val list = json["list"]?.jsonArray ?: throw IllegalStateException("Invalid forecast response")

        return list.mapNotNull { item ->
            item.jsonObject.let { forecast ->
                val main = forecast["main"]?.jsonObject ?: return@mapNotNull null
                val weather = forecast["weather"]?.jsonArray ?: return@mapNotNull null
                val wind = forecast["wind"]?.jsonObject ?: return@mapNotNull null
                val clouds = forecast["clouds"]?.jsonObject ?: return@mapNotNull null
                val rain = forecast["rain"]?.jsonObject

                WeatherData(
                    temperature = main["temp"]?.jsonPrimitive?.double ?: 0.0,
                    humidity = main["humidity"]?.jsonPrimitive?.double?.toDouble() ?: 0.0,
                    pressure = main["pressure"]?.jsonPrimitive?.double?.toDouble() ?: 0.0,
                    windSpeed = wind["speed"]?.jsonPrimitive?.double ?: 0.0,
                    cloudiness = clouds["all"]?.jsonPrimitive?.double?.toDouble() ?: 0.0,
                    precipitation = rain?.get("3h")?.jsonPrimitive?.double ?: 0.0,
                    description = weather.firstOrNull()?.jsonObject?.get("description")?.jsonPrimitive?.content ?: "",
                    timestamp = forecast["dt"]?.jsonPrimitive?.long ?: Clock.System.now().epochSeconds
                )
            }
        }
    }
}
