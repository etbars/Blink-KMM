package com.example.alertapp.api.weather

import com.example.alertapp.api.ApiError
import com.example.alertapp.api.ApiResponse
import com.example.alertapp.api.BaseApiProvider
import com.example.alertapp.models.weather.*
import kotlinx.serialization.json.*
import kotlinx.datetime.*
import co.touchlab.kermit.Logger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class OpenWeatherProvider(
    override val config: WeatherConfig
) : BaseApiProvider<WeatherConfig>() {
    private val logger = Logger.withTag("OpenWeatherProvider")
    private val cache = WeatherCache()

    suspend fun getCurrentWeather(
        latitude: Double,
        longitude: Double
    ): Flow<ApiResponse<WeatherData.Current>> = flow {
        try {
            logger.d { "Fetching current weather for lat: $latitude, lon: $longitude" }
            
            // Check cache first
            val cacheKey = "current:$latitude:$longitude"
            cache.getCachedWeather<WeatherData.Current>(cacheKey)?.let { weather ->
                logger.d { "Returning cached current weather" }
                emit(ApiResponse.Success(weather))
                return@flow
            }

            // Make API request
            val response = get<JsonObject>(
                endpoint = "weather",
                params = mapOf(
                    "lat" to latitude.toString(),
                    "lon" to longitude.toString(),
                    "units" to config.units
                )
            )

            response.collect { apiResponse ->
                when (apiResponse) {
                    is ApiResponse.Success -> {
                        val weather = parseCurrentWeatherResponse(apiResponse.data)
                        cache.cacheWeather(cacheKey, weather)
                        emit(ApiResponse.Success(weather))
                    }
                    is ApiResponse.Loading -> emit(ApiResponse.Loading)
                    is ApiResponse.Error -> emit(apiResponse)
                }
            }
        } catch (e: Exception) {
            logger.e(e) { "Error fetching current weather" }
            emit(ApiResponse.Error(
                message = "Failed to fetch current weather",
                cause = e
            ))
        }
    }

    suspend fun getForecast(
        latitude: Double,
        longitude: Double
    ): Flow<ApiResponse<WeatherData.Forecast>> = flow {
        try {
            logger.d { "Fetching forecast for lat: $latitude, lon: $longitude" }
            
            // Check cache first
            val cacheKey = "forecast:$latitude:$longitude"
            cache.getCachedWeather<WeatherData.Forecast>(cacheKey)?.let { forecast ->
                logger.d { "Returning cached forecast" }
                emit(ApiResponse.Success(forecast))
                return@flow
            }

            // Make API request
            val response = get<JsonObject>(
                endpoint = "onecall",
                params = mapOf(
                    "lat" to latitude.toString(),
                    "lon" to longitude.toString(),
                    "units" to config.units,
                    "exclude" to "minutely,current"
                )
            )

            response.collect { apiResponse ->
                when (apiResponse) {
                    is ApiResponse.Success -> {
                        val forecast = parseForecastResponse(apiResponse.data)
                        cache.cacheWeather(cacheKey, forecast)
                        emit(ApiResponse.Success(forecast))
                    }
                    is ApiResponse.Loading -> emit(ApiResponse.Loading)
                    is ApiResponse.Error -> emit(apiResponse)
                }
            }
        } catch (e: Exception) {
            logger.e(e) { "Error fetching forecast" }
            emit(ApiResponse.Error(
                message = "Failed to fetch forecast",
                cause = e
            ))
        }
    }

    suspend fun getAlerts(
        latitude: Double,
        longitude: Double
    ): Flow<ApiResponse<List<WeatherData.Alert>>> = flow {
        try {
            logger.d { "Fetching weather alerts for lat: $latitude, lon: $longitude" }
            
            // Check cache first
            val cacheKey = "alerts:$latitude:$longitude"
            cache.getCachedWeather<List<WeatherData.Alert>>(cacheKey)?.let { alerts ->
                logger.d { "Returning cached alerts" }
                emit(ApiResponse.Success(alerts))
                return@flow
            }

            // Make API request
            val response = get<JsonObject>(
                endpoint = "onecall",
                params = mapOf(
                    "lat" to latitude.toString(),
                    "lon" to longitude.toString(),
                    "exclude" to "current,minutely,hourly,daily"
                )
            )

            response.collect { apiResponse ->
                when (apiResponse) {
                    is ApiResponse.Success -> {
                        val alerts = parseAlertsResponse(apiResponse.data)
                        cache.cacheWeather(cacheKey, alerts)
                        emit(ApiResponse.Success(alerts))
                    }
                    is ApiResponse.Loading -> emit(ApiResponse.Loading)
                    is ApiResponse.Error -> emit(apiResponse)
                }
            }
        } catch (e: Exception) {
            logger.e(e) { "Error fetching weather alerts" }
            emit(ApiResponse.Error(
                message = "Failed to fetch weather alerts",
                cause = e
            ))
        }
    }

    private fun parseCurrentWeatherResponse(json: JsonObject): WeatherData.Current {
        try {
            val main = json["main"]?.jsonObject ?: throw ApiError.ParseError("Missing main data")
            val wind = json["wind"]?.jsonObject ?: throw ApiError.ParseError("Missing wind data")
            val weather = json["weather"]?.jsonArray?.firstOrNull()?.jsonObject 
                ?: throw ApiError.ParseError("Missing weather data")
            val sys = json["sys"]?.jsonObject ?: throw ApiError.ParseError("Missing system data")
            val coord = json["coord"]?.jsonObject ?: throw ApiError.ParseError("Missing coordinates")

            val location = WeatherLocation(
                latitude = coord["lat"]?.jsonPrimitive?.double 
                    ?: throw ApiError.ParseError("Missing latitude"),
                longitude = coord["lon"]?.jsonPrimitive?.double 
                    ?: throw ApiError.ParseError("Missing longitude"),
                name = json["name"]?.jsonPrimitive?.contentOrNull,
                country = sys["country"]?.jsonPrimitive?.contentOrNull,
                timezone = TimeZone.of(json["timezone"]?.jsonPrimitive?.content ?: "UTC")
            )

            return WeatherData.Current(
                temperature = main["temp"]?.jsonPrimitive?.double 
                    ?: throw ApiError.ParseError("Missing temperature"),
                feelsLike = main["feels_like"]?.jsonPrimitive?.double 
                    ?: throw ApiError.ParseError("Missing feels like temperature"),
                humidity = main["humidity"]?.jsonPrimitive?.int 
                    ?: throw ApiError.ParseError("Missing humidity"),
                pressure = main["pressure"]?.jsonPrimitive?.int 
                    ?: throw ApiError.ParseError("Missing pressure"),
                windSpeed = wind["speed"]?.jsonPrimitive?.double 
                    ?: throw ApiError.ParseError("Missing wind speed"),
                windDirection = wind["deg"]?.jsonPrimitive?.int 
                    ?: throw ApiError.ParseError("Missing wind direction"),
                description = weather["description"]?.jsonPrimitive?.content 
                    ?: throw ApiError.ParseError("Missing weather description"),
                icon = weather["icon"]?.jsonPrimitive?.content 
                    ?: throw ApiError.ParseError("Missing weather icon"),
                timestamp = Instant.fromEpochSeconds(
                    json["dt"]?.jsonPrimitive?.long 
                        ?: throw ApiError.ParseError("Missing timestamp")
                ),
                location = location
            )
        } catch (e: Exception) {
            logger.e(e) { "Error parsing current weather response" }
            throw ApiError.ParseError("Failed to parse current weather data: ${e.message}")
        }
    }

    private fun parseForecastResponse(json: JsonObject): WeatherData.Forecast {
        try {
            val location = WeatherLocation(
                latitude = json["lat"]?.jsonPrimitive?.double 
                    ?: throw ApiError.ParseError("Missing latitude"),
                longitude = json["lon"]?.jsonPrimitive?.double 
                    ?: throw ApiError.ParseError("Missing longitude"),
                name = null,
                country = null,
                timezone = TimeZone.of(
                    json["timezone"]?.jsonPrimitive?.content 
                        ?: throw ApiError.ParseError("Missing timezone")
                )
            )

            val hourlyForecasts = json["hourly"]?.jsonArray?.map { hourly ->
                val item = hourly.jsonObject
                val weather = item["weather"]?.jsonArray?.firstOrNull()?.jsonObject 
                    ?: throw ApiError.ParseError("Missing weather data in hourly forecast")

                HourlyForecast(
                    timestamp = Instant.fromEpochSeconds(
                        item["dt"]?.jsonPrimitive?.long 
                            ?: throw ApiError.ParseError("Missing timestamp")
                    ),
                    temperature = item["temp"]?.jsonPrimitive?.double 
                        ?: throw ApiError.ParseError("Missing temperature"),
                    feelsLike = item["feels_like"]?.jsonPrimitive?.double 
                        ?: throw ApiError.ParseError("Missing feels like temperature"),
                    humidity = item["humidity"]?.jsonPrimitive?.int 
                        ?: throw ApiError.ParseError("Missing humidity"),
                    pressure = item["pressure"]?.jsonPrimitive?.int 
                        ?: throw ApiError.ParseError("Missing pressure"),
                    windSpeed = item["wind_speed"]?.jsonPrimitive?.double 
                        ?: throw ApiError.ParseError("Missing wind speed"),
                    windDirection = item["wind_deg"]?.jsonPrimitive?.int 
                        ?: throw ApiError.ParseError("Missing wind direction"),
                    description = weather["description"]?.jsonPrimitive?.content 
                        ?: throw ApiError.ParseError("Missing weather description"),
                    icon = weather["icon"]?.jsonPrimitive?.content 
                        ?: throw ApiError.ParseError("Missing weather icon"),
                    precipitationProbability = item["pop"]?.jsonPrimitive?.double ?: 0.0,
                    precipitation = (item["rain"]?.jsonPrimitive?.double ?: 0.0) + 
                        (item["snow"]?.jsonPrimitive?.double ?: 0.0)
                )
            } ?: throw ApiError.ParseError("Missing hourly forecast data")

            val dailyForecasts = json["daily"]?.jsonArray?.map { daily ->
                val item = daily.jsonObject
                val temp = item["temp"]?.jsonObject 
                    ?: throw ApiError.ParseError("Missing temperature data in daily forecast")
                val weather = item["weather"]?.jsonArray?.firstOrNull()?.jsonObject 
                    ?: throw ApiError.ParseError("Missing weather data in daily forecast")

                DailyForecast(
                    date = Instant.fromEpochSeconds(
                        item["dt"]?.jsonPrimitive?.long 
                            ?: throw ApiError.ParseError("Missing timestamp")
                    ).toLocalDateTime(location.timezone),
                    temperatureMin = temp["min"]?.jsonPrimitive?.double 
                        ?: throw ApiError.ParseError("Missing minimum temperature"),
                    temperatureMax = temp["max"]?.jsonPrimitive?.double 
                        ?: throw ApiError.ParseError("Missing maximum temperature"),
                    humidity = item["humidity"]?.jsonPrimitive?.int 
                        ?: throw ApiError.ParseError("Missing humidity"),
                    pressure = item["pressure"]?.jsonPrimitive?.int 
                        ?: throw ApiError.ParseError("Missing pressure"),
                    windSpeed = item["wind_speed"]?.jsonPrimitive?.double 
                        ?: throw ApiError.ParseError("Missing wind speed"),
                    windDirection = item["wind_deg"]?.jsonPrimitive?.int 
                        ?: throw ApiError.ParseError("Missing wind direction"),
                    description = weather["description"]?.jsonPrimitive?.content 
                        ?: throw ApiError.ParseError("Missing weather description"),
                    icon = weather["icon"]?.jsonPrimitive?.content 
                        ?: throw ApiError.ParseError("Missing weather icon"),
                    precipitationProbability = item["pop"]?.jsonPrimitive?.double ?: 0.0,
                    precipitation = (item["rain"]?.jsonPrimitive?.double ?: 0.0) + 
                        (item["snow"]?.jsonPrimitive?.double ?: 0.0),
                    sunrise = Instant.fromEpochSeconds(
                        item["sunrise"]?.jsonPrimitive?.long 
                            ?: throw ApiError.ParseError("Missing sunrise time")
                    ),
                    sunset = Instant.fromEpochSeconds(
                        item["sunset"]?.jsonPrimitive?.long 
                            ?: throw ApiError.ParseError("Missing sunset time")
                    )
                )
            } ?: throw ApiError.ParseError("Missing daily forecast data")

            return WeatherData.Forecast(
                hourly = hourlyForecasts,
                daily = dailyForecasts,
                location = location
            )
        } catch (e: Exception) {
            logger.e(e) { "Error parsing forecast response" }
            throw ApiError.ParseError("Failed to parse forecast data: ${e.message}")
        }
    }

    private fun parseAlertsResponse(json: JsonObject): List<WeatherData.Alert> {
        try {
            val location = WeatherLocation(
                latitude = json["lat"]?.jsonPrimitive?.double 
                    ?: throw ApiError.ParseError("Missing latitude"),
                longitude = json["lon"]?.jsonPrimitive?.double 
                    ?: throw ApiError.ParseError("Missing longitude"),
                name = null,
                country = null,
                timezone = TimeZone.of(
                    json["timezone"]?.jsonPrimitive?.content 
                        ?: throw ApiError.ParseError("Missing timezone")
                )
            )

            return json["alerts"]?.jsonArray?.map { alert ->
                val alertObj = alert.jsonObject
                WeatherData.Alert(
                    title = alertObj["event"]?.jsonPrimitive?.content 
                        ?: throw ApiError.ParseError("Missing alert event"),
                    description = alertObj["description"]?.jsonPrimitive?.content 
                        ?: throw ApiError.ParseError("Missing alert description"),
                    severity = when (alertObj["severity"]?.jsonPrimitive?.content?.lowercase()) {
                        "moderate" -> AlertSeverity.MODERATE
                        "severe" -> AlertSeverity.SEVERE
                        "extreme" -> AlertSeverity.EXTREME
                        else -> AlertSeverity.MINOR
                    },
                    start = Instant.fromEpochSeconds(
                        alertObj["start"]?.jsonPrimitive?.long 
                            ?: throw ApiError.ParseError("Missing alert start time")
                    ),
                    end = Instant.fromEpochSeconds(
                        alertObj["end"]?.jsonPrimitive?.long 
                            ?: throw ApiError.ParseError("Missing alert end time")
                    ),
                    location = location
                )
            } ?: emptyList()
        } catch (e: Exception) {
            logger.e(e) { "Error parsing alerts response" }
            throw ApiError.ParseError("Failed to parse weather alerts: ${e.message}")
        }
    }

    private class WeatherCache {
        private val cache = mutableMapOf<String, CacheEntry<Any>>()

        @Suppress("UNCHECKED_CAST")
        fun <T> getCachedWeather(key: String): T? {
            val entry = cache[key] ?: return null
            if (entry.isExpired()) {
                cache.remove(key)
                return null
            }
            return entry.data as? T
        }

        fun cacheWeather(key: String, data: Any) {
            cache[key] = CacheEntry(data)
        }

        private data class CacheEntry<T>(
            val data: T,
            val timestamp: Instant = Clock.System.now()
        ) {
            fun isExpired(): Boolean =
                Clock.System.now() - timestamp > 5.minutes
        }
    }
}
