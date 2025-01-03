package com.example.alertapp.network.services

import com.example.alertapp.api.ApiResponse
import com.example.alertapp.api.errors.ApiError
import com.example.alertapp.network.NetworkClient
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.HttpStatusCode
import kotlinx.datetime.*
import kotlinx.serialization.Serializable

class PriceApiService(
    private val networkClient: NetworkClient,
    private val apiKey: String,
    private val baseUrl: String = "https://www.alphavantage.co/query"
) {
    suspend fun getPriceHistory(
        asset: String,
        timeframeMinutes: Int
    ): ApiResponse<List<PriceDataPoint>> {
        return try {
            val interval = when {
                timeframeMinutes <= 60 -> "1min"
                timeframeMinutes <= 60 * 24 -> "5min"
                timeframeMinutes <= 60 * 24 * 7 -> "15min"
                else -> "60min"
            }

            val response = networkClient.client.get(baseUrl) {
                parameter("function", "TIME_SERIES_INTRADAY")
                parameter("symbol", asset)
                parameter("interval", interval)
                parameter("apikey", apiKey)
                parameter("outputsize", "full")
            }

            when (response.status) {
                HttpStatusCode.OK -> {
                    val priceResponse = response.body<Map<String, Map<String, Map<String, String>>>>()
                    val timeSeriesKey = priceResponse.keys.find { it.startsWith("Time Series") }
                        ?: return ApiResponse.Error(ApiError.ParseError("Time series data not found in response"))
                    
                    val timeSeries = priceResponse[timeSeriesKey] ?: emptyMap()
                    if (timeSeries.isEmpty()) {
                        return ApiResponse.Error(ApiError.NoDataError("No price data available for $asset"))
                    }

                    ApiResponse.Success(timeSeries.map { (timestamp, values) ->
                        PriceDataPoint(
                            timestamp = parseTimestamp(timestamp),
                            price = values["4. close"]?.toDoubleOrNull() ?: 0.0,
                            volume = values["5. volume"]?.toDoubleOrNull(),
                            openPrice = values["1. open"]?.toDoubleOrNull(),
                            highPrice = values["2. high"]?.toDoubleOrNull(),
                            lowPrice = values["3. low"]?.toDoubleOrNull()
                        )
                    }.sortedBy { it.timestamp })
                }
                HttpStatusCode.Unauthorized -> ApiResponse.Error(ApiError.AuthenticationError("Invalid API key"))
                HttpStatusCode.TooManyRequests -> ApiResponse.Error(ApiError.RateLimitError("Rate limit exceeded"))
                else -> ApiResponse.Error(ApiError.ServerError("Server returned ${response.status}"))
            }
        } catch (e: Exception) {
            ApiResponse.Error(ApiError.NetworkError("Failed to fetch price data: ${e.message}"))
        }
    }

    suspend fun getCurrentPrice(asset: String): ApiResponse<PriceDataPoint> {
        return try {
            val response = networkClient.client.get(baseUrl) {
                parameter("function", "GLOBAL_QUOTE")
                parameter("symbol", asset)
                parameter("apikey", apiKey)
            }

            when (response.status) {
                HttpStatusCode.OK -> {
                    val priceResponse = response.body<Map<String, Map<String, String>>>()
                    val quote = priceResponse["Global Quote"] 
                        ?: return ApiResponse.Error(ApiError.ParseError("Quote data not found in response"))

                    if (quote.isEmpty()) {
                        return ApiResponse.Error(ApiError.NoDataError("No price data available for $asset"))
                    }

                    ApiResponse.Success(PriceDataPoint(
                        timestamp = Clock.System.now(),
                        price = quote["05. price"]?.toDoubleOrNull() ?: 0.0,
                        volume = quote["06. volume"]?.toDoubleOrNull(),
                        openPrice = quote["02. open"]?.toDoubleOrNull(),
                        highPrice = quote["03. high"]?.toDoubleOrNull(),
                        lowPrice = quote["04. low"]?.toDoubleOrNull()
                    ))
                }
                HttpStatusCode.Unauthorized -> ApiResponse.Error(ApiError.AuthenticationError("Invalid API key"))
                HttpStatusCode.TooManyRequests -> ApiResponse.Error(ApiError.RateLimitError("Rate limit exceeded"))
                else -> ApiResponse.Error(ApiError.ServerError("Server returned ${response.status}"))
            }
        } catch (e: Exception) {
            ApiResponse.Error(ApiError.NetworkError("Failed to fetch current price: ${e.message}"))
        }
    }

    private fun parseTimestamp(timestamp: String): Instant {
        return try {
            Instant.parse(timestamp)
        } catch (e: Exception) {
            Clock.System.now()
        }
    }
}

@Serializable
data class PriceDataPoint(
    val timestamp: Instant,
    val price: Double,
    val volume: Double? = null,
    val openPrice: Double? = null,
    val highPrice: Double? = null,
    val lowPrice: Double? = null
)
