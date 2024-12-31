package com.example.alertapp.network.services

import com.example.alertapp.network.ApiResponse
import com.example.alertapp.network.NetworkClient
import io.ktor.client.call.*
import io.ktor.client.request.*
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

            val priceResponse = response.body<Map<String, Map<String, Map<String, String>>>>()
            val timeSeriesKey = priceResponse.keys.find { it.startsWith("Time Series") }
                ?: throw IllegalStateException("Time series data not found in response")
            
            val timeSeries = priceResponse[timeSeriesKey] ?: emptyMap()
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
        } catch (e: Exception) {
            ApiResponse.Error(message = "Failed to fetch price data: ${e.message}")
        }
    }

    suspend fun getCurrentPrice(asset: String): ApiResponse<PriceDataPoint> {
        return try {
            val response = networkClient.client.get(baseUrl) {
                parameter("function", "GLOBAL_QUOTE")
                parameter("symbol", asset)
                parameter("apikey", apiKey)
            }

            val quoteResponse = response.body<Map<String, Map<String, String>>>()
            val quote = quoteResponse["Global Quote"] 
                ?: throw IllegalStateException("Quote data not found in response")

            ApiResponse.Success(PriceDataPoint(
                timestamp = Clock.System.now(),
                price = quote["05. price"]?.toDoubleOrNull() ?: 0.0,
                volume = quote["06. volume"]?.toDoubleOrNull(),
                openPrice = quote["02. open"]?.toDoubleOrNull(),
                highPrice = quote["03. high"]?.toDoubleOrNull(),
                lowPrice = quote["04. low"]?.toDoubleOrNull(),
                changePercent = quote["10. change percent"]?.removeSuffix("%")?.toDoubleOrNull()
            ))
        } catch (e: Exception) {
            ApiResponse.Error(message = "Failed to fetch current price: ${e.message}")
        }
    }

    private fun parseTimestamp(timestamp: String): Instant {
        return try {
            // Parse ISO 8601 timestamp (e.g., "2024-01-01 12:00:00")
            val (date, time) = timestamp.split(" ")
            val (year, month, day) = date.split("-").map { it.toInt() }
            val (hour, minute, second) = time.split(":").map { it.toInt() }
            
            LocalDateTime(
                year = year,
                monthNumber = month,
                dayOfMonth = day,
                hour = hour,
                minute = minute,
                second = second
            ).toInstant(TimeZone.UTC)
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
    val lowPrice: Double? = null,
    val changePercent: Double? = null
)
