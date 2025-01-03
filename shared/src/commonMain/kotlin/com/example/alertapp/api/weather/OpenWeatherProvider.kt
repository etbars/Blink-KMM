package com.example.alertapp.api.weather

import com.example.alertapp.api.ApiResponse
import com.example.alertapp.api.BaseApiProvider
import com.example.alertapp.api.errors.ApiError
import com.example.alertapp.models.weather.WeatherData
import com.example.alertapp.models.weather.HourlyForecast
import com.example.alertapp.models.weather.DailyForecast
import com.example.alertapp.config.AppConfig
import com.example.alertapp.network.NetworkClient
import io.ktor.client.call.*
import io.ktor.http.*
import kotlinx.datetime.Instant
import kotlinx.serialization.json.*
import co.touchlab.kermit.Logger

class OpenWeatherProvider(
    networkClient: NetworkClient,
    override val config: Map<String, String>,
    private val appConfig: AppConfig
) : BaseApiProvider(networkClient) {
    
    override val baseUrl: String = config["WEATHER_API_URL"] ?: throw IllegalStateException("WEATHER_API_URL not configured")
    override val logger: Logger = Logger.withTag("OpenWeatherProvider")

    suspend fun getCurrentWeather(lat: Double, lon: Double): ApiResponse<com.example.alertapp.models.weather.WeatherData.Current> {
        return get<JsonObject>(
            endpoint = "weather",
            params = mapOf(
                "lat" to lat.toString(),
                "lon" to lon.toString()
            )
        ).let { response ->
            when (response) {
                is ApiResponse.Success -> {
                    val json = response.data
                    val main = json["main"]?.jsonObject
                    val wind = json["wind"]?.jsonObject
                    val weather = json["weather"]?.jsonArray?.firstOrNull()?.jsonObject
                    val clouds = json["clouds"]?.jsonObject
                    val rain = json["rain"]?.jsonObject

                    if (main == null || wind == null || weather == null) {
                        ApiResponse.Error(ApiError.ParseError("Invalid response format"))
                    } else try {
                        ApiResponse.Success(
                            com.example.alertapp.models.weather.WeatherData.Current(
                                timestamp = json["dt"]?.jsonPrimitive?.longOrNull?.let { Instant.fromEpochSeconds(it) }
                                    ?: throw Exception("Invalid timestamp format"),
                                temperature = main["temp"]?.jsonPrimitive?.doubleOrNull
                                    ?: throw Exception("Invalid temperature format"),
                                feelsLike = main["feels_like"]?.jsonPrimitive?.doubleOrNull
                                    ?: throw Exception("Invalid feels like format"),
                                humidity = main["humidity"]?.jsonPrimitive?.intOrNull ?: 0,
                                pressure = main["pressure"]?.jsonPrimitive?.intOrNull ?: 0,
                                windSpeed = wind["speed"]?.jsonPrimitive?.doubleOrNull ?: 0.0,
                                windDirection = wind["deg"]?.jsonPrimitive?.intOrNull ?: 0,
                                description = weather["description"]?.jsonPrimitive?.content ?: "Unknown",
                                cloudCover = clouds?.get("all")?.jsonPrimitive?.intOrNull,
                                precipitation = rain?.get("1h")?.jsonPrimitive?.doubleOrNull
                            )
                        )
                    } catch (e: Exception) {
                        ApiResponse.Error(ApiError.ParseError(e.message ?: "Failed to parse weather data"))
                    }
                }
                is ApiResponse.Error -> response
                is ApiResponse.Loading -> response
            }
        }
    }

    suspend fun getForecast(lat: Double, lon: Double): ApiResponse<com.example.alertapp.models.weather.WeatherData.Forecast> {
        return get<JsonObject>(
            endpoint = "forecast",
            params = mapOf(
                "lat" to lat.toString(),
                "lon" to lon.toString()
            )
        ).let { response ->
            when (response) {
                is ApiResponse.Success -> {
                    val json = response.data
                    val list = json["list"]?.jsonArray

                    if (list == null) {
                        ApiResponse.Error(ApiError.ParseError("Invalid response format"))
                    } else try {
                        val hourlyForecasts = list.mapNotNull { item ->
                            val itemObj = item.jsonObject
                            val main = itemObj["main"]?.jsonObject ?: return@mapNotNull null
                            val wind = itemObj["wind"]?.jsonObject ?: return@mapNotNull null
                            val weather = itemObj["weather"]?.jsonArray?.firstOrNull()?.jsonObject ?: return@mapNotNull null
                            val clouds = itemObj["clouds"]?.jsonObject
                            val rain = itemObj["rain"]?.jsonObject

                            HourlyForecast(
                                timestamp = itemObj["dt"]?.jsonPrimitive?.longOrNull?.let { Instant.fromEpochSeconds(it) }
                                    ?: return@mapNotNull null,
                                temperature = main["temp"]?.jsonPrimitive?.doubleOrNull ?: return@mapNotNull null,
                                feelsLike = main["feels_like"]?.jsonPrimitive?.doubleOrNull ?: return@mapNotNull null,
                                humidity = main["humidity"]?.jsonPrimitive?.intOrNull ?: 0,
                                pressure = main["pressure"]?.jsonPrimitive?.intOrNull ?: 0,
                                windSpeed = wind["speed"]?.jsonPrimitive?.doubleOrNull ?: 0.0,
                                windDirection = wind["deg"]?.jsonPrimitive?.intOrNull ?: 0,
                                description = weather["description"]?.jsonPrimitive?.content ?: "Unknown",
                                cloudCover = clouds?.get("all")?.jsonPrimitive?.intOrNull,
                                precipitation = rain?.get("1h")?.jsonPrimitive?.doubleOrNull,
                                icon = weather["icon"]?.jsonPrimitive?.content ?: "unknown"
                            )
                        }

                        val dailyForecasts = hourlyForecasts
                            .groupBy { it.timestamp.epochSeconds / (24 * 3600) }
                            .map { (_, forecasts) ->
                                DailyForecast(
                                    timestamp = forecasts.first().timestamp,
                                    tempMin = forecasts.minOf { it.temperature },
                                    tempMax = forecasts.maxOf { it.temperature },
                                    humidity = forecasts.fold(0) { acc, forecast -> acc + forecast.humidity } / forecasts.size,
                                    pressure = forecasts.fold(0) { acc, forecast -> acc + forecast.pressure } / forecasts.size,
                                    windSpeed = forecasts.fold(0.0) { acc, forecast -> acc + forecast.windSpeed } / forecasts.size,
                                    windDirection = forecasts.fold(0) { acc, forecast -> acc + forecast.windDirection } / forecasts.size,
                                    description = forecasts.first().description,
                                    cloudCover = forecasts.firstNotNullOf { it.cloudCover },
                                    precipitation = forecasts.firstNotNullOf { it.precipitation },
                                    icon = forecasts.first().icon
                                )
                            }

                        ApiResponse.Success(
                            com.example.alertapp.models.weather.WeatherData.Forecast(
                                hourly = hourlyForecasts,
                                daily = dailyForecasts
                            )
                        )
                    } catch (e: Exception) {
                        ApiResponse.Error(ApiError.ParseError(e.message ?: "Failed to parse forecast data"))
                    }
                }
                is ApiResponse.Error -> response
                is ApiResponse.Loading -> response
            }
        }
    }
}
