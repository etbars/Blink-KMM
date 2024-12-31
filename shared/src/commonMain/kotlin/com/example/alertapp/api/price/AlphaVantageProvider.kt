package com.example.alertapp.api.price

import com.example.alertapp.api.ApiError
import com.example.alertapp.api.ApiResponse
import com.example.alertapp.api.BaseApiProvider
import com.example.alertapp.models.price.PriceData
import com.example.alertapp.models.price.OHLCV
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.datetime.Instant
import kotlinx.serialization.json.*
import co.touchlab.kermit.Logger

class AlphaVantageProvider(
    override val config: PriceConfig
) : BaseApiProvider<PriceConfig>(
) {
    private val logger = Logger.withTag("AlphaVantageProvider")

    suspend fun getQuote(symbol: String): Flow<ApiResponse<PriceData.Quote>> = flow {
        emit(ApiResponse.Loading)
        try {
            val response = get<JsonObject>(
                endpoint = "query",
                params = mapOf(
                    "function" to "GLOBAL_QUOTE",
                    "symbol" to symbol,
                    "apikey" to config.apiKey
                )
            )

            response.collect { apiResponse ->
                when (apiResponse) {
                    is ApiResponse.Success -> {
                        val quote = parseQuoteResponse(apiResponse.data)
                        emit(ApiResponse.success(quote))
                    }
                    is ApiResponse.Loading -> emit(ApiResponse.Loading)
                    is ApiResponse.Error -> emit(apiResponse)
                }
            }
        } catch (e: Exception) {
            logger.e(e) { "Error getting quote for $symbol" }
            emit(ApiResponse.error(ApiError.UnknownError(e.message ?: "Unknown error")))
        }
    }

    suspend fun getHistoricalPrices(
        symbol: String,
        interval: String = "5min"
    ): Flow<ApiResponse<PriceData.Historical>> = flow {
        emit(ApiResponse.Loading)
        try {
            val response = get<JsonObject>(
                endpoint = "query",
                params = mapOf(
                    "function" to "TIME_SERIES_INTRADAY",
                    "symbol" to symbol,
                    "interval" to interval,
                    "apikey" to config.apiKey
                )
            )

            response.collect { apiResponse ->
                when (apiResponse) {
                    is ApiResponse.Success -> {
                        val historical = parseHistoricalResponse(apiResponse.data, interval)
                        emit(ApiResponse.success(historical))
                    }
                    is ApiResponse.Loading -> emit(ApiResponse.Loading)
                    is ApiResponse.Error -> emit(apiResponse)
                }
            }
        } catch (e: Exception) {
            logger.e(e) { "Error getting historical prices for $symbol" }
            emit(ApiResponse.error(ApiError.UnknownError(e.message ?: "Unknown error")))
        }
    }

    private fun parseQuoteResponse(jsonObject: JsonObject): PriceData.Quote {
        val quoteData = jsonObject["Global Quote"]?.jsonObject
            ?: throw ApiError.ParseError("Global Quote not found in response")

        return PriceData.Quote(
            symbol = quoteData["01. symbol"]?.jsonPrimitive?.content
                ?: throw ApiError.ParseError("Symbol not found in response"),
            currentPrice = quoteData["05. price"]?.jsonPrimitive?.content?.toDoubleOrNull()
                ?: throw ApiError.ParseError("Price not found in response"),
            priceChange = quoteData["09. change"]?.jsonPrimitive?.content?.toDoubleOrNull() ?: 0.0,
            percentChange = quoteData["10. change percent"]?.jsonPrimitive?.content?.removeSuffix("%")?.toDoubleOrNull() ?: 0.0,
            volume = quoteData["06. volume"]?.jsonPrimitive?.content?.toLongOrNull() ?: 0L,
            timestamp = Instant.parse(quoteData["07. latest trading day"]?.jsonPrimitive?.content?.plus("T00:00:00Z")
                ?: throw ApiError.ParseError("Timestamp not found in response"))
        )
    }

    private fun parseHistoricalResponse(jsonObject: JsonObject, interval: String): PriceData.Historical {
        val metadata = jsonObject["Meta Data"]?.jsonObject
            ?: throw ApiError.ParseError("Meta Data not found in response")
        
        val symbol = metadata["2. Symbol"]?.jsonPrimitive?.content
            ?: throw ApiError.ParseError("Symbol not found in metadata")

        val timeSeriesKey = when (interval) {
            "1min" -> "Time Series (1min)"
            "5min" -> "Time Series (5min)"
            "15min" -> "Time Series (15min)"
            "30min" -> "Time Series (30min)"
            "60min" -> "Time Series (60min)"
            else -> throw ApiError.ValidationError("Invalid interval: $interval")
        }

        val timeSeries = jsonObject[timeSeriesKey]?.jsonObject
            ?: throw ApiError.ParseError("Time series data not found")

        val prices = timeSeries.entries.mapNotNull { (timestamp, data) ->
            try {
                val dataObj = data.jsonObject
                OHLCV(
                    timestamp = Instant.parse(timestamp.replace(" ", "T") + "Z"),
                    open = dataObj["1. open"]?.jsonPrimitive?.content?.toDoubleOrNull() ?: return@mapNotNull null,
                    high = dataObj["2. high"]?.jsonPrimitive?.content?.toDoubleOrNull() ?: return@mapNotNull null,
                    low = dataObj["3. low"]?.jsonPrimitive?.content?.toDoubleOrNull() ?: return@mapNotNull null,
                    close = dataObj["4. close"]?.jsonPrimitive?.content?.toDoubleOrNull() ?: return@mapNotNull null,
                    volume = dataObj["5. volume"]?.jsonPrimitive?.content?.toLongOrNull() ?: 0L
                )
            } catch (e: Exception) {
                logger.w(e) { "Failed to parse price point for timestamp $timestamp" }
                null
            }
        }

        if (prices.isEmpty()) {
            throw ApiError.ValidationError("No valid price data found")
        }

        return PriceData.Historical(
            symbol = symbol,
            interval = interval,
            prices = prices.sortedBy { it.timestamp },
            startTime = prices.first().timestamp,
            endTime = prices.last().timestamp
        )
    }
}
