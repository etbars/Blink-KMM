package com.example.alertapp.android.api

import com.example.alertapp.android.cache.CacheManager
import com.example.alertapp.android.processors.PriceData
import com.google.gson.JsonParser
import kotlinx.coroutines.delay
import retrofit2.HttpException
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

interface PriceApi {
    @GET("query")
    suspend fun getCurrentPrice(
        @Query("function") function: String = "GLOBAL_QUOTE",
        @Query("symbol") symbol: String,
        @Query("apikey") apiKey: String
    ): Response<AlphaVantageResponse>
}

data class AlphaVantageResponse(
    val `Global Quote`: GlobalQuote?
) {
    data class GlobalQuote(
        val `01. symbol`: String,
        val `02. open`: String,
        val `03. high`: String,
        val `04. low`: String,
        val `05. price`: String,
        val `06. volume`: String,
        val `07. latest trading day`: String,
        val `08. previous close`: String,
        val `09. change`: String,
        val `10. change percent`: String
    )
}

@Singleton
class PriceApiImpl @Inject constructor(
    private val api: PriceApi,
    private val apiKey: String,
    private val rateLimiter: RateLimiter,
    private val cacheManager: CacheManager
) {
    companion object {
        private const val RATE_LIMIT_KEY = "alphavantage"
        private const val MAX_REQUESTS = 5
        private const val TIME_WINDOW_MILLIS = 60_000L // 1 minute
        private const val MAX_RETRIES = 3
        private const val RETRY_DELAY_MS = 1000L
    }

    suspend fun getCurrentPrice(symbol: String): ApiResult<PriceData> {
        // Check cache first
        cacheManager.getPriceData(symbol)?.let { cachedData ->
            return ApiResult.Success(cachedData)
        }

        // Check rate limit
        if (!rateLimiter.checkRateLimit(RATE_LIMIT_KEY, MAX_REQUESTS, TIME_WINDOW_MILLIS)) {
            val waitTime = rateLimiter.getRemainingWaitTime(RATE_LIMIT_KEY, MAX_REQUESTS, TIME_WINDOW_MILLIS)
            return ApiResult.Error(
                ErrorCode.RATE_LIMIT_EXCEEDED,
                "Rate limit exceeded. Please wait ${rateLimiter.getFormattedWaitTime(waitTime)}.",
                waitTime
            )
        }

        var lastException: Exception? = null
        repeat(MAX_RETRIES) { attempt ->
            try {
                val response = api.getCurrentPrice(symbol = symbol, apiKey = apiKey)
                
                if (!response.isSuccessful) {
                    // Check for rate limit in response
                    val errorBody = response.errorBody()?.string()
                    if (errorBody?.contains("API call frequency") == true) {
                        val waitTime = rateLimiter.getRemainingWaitTime(RATE_LIMIT_KEY, MAX_REQUESTS, TIME_WINDOW_MILLIS)
                        return ApiResult.Error(
                            ErrorCode.RATE_LIMIT_EXCEEDED,
                            "API rate limit exceeded. Please wait ${rateLimiter.getFormattedWaitTime(waitTime)}.",
                            waitTime
                        )
                    }
                    
                    return ApiResult.Error(
                        ErrorCode.API_ERROR,
                        "API error: ${response.code()} - ${response.message()}"
                    )
                }

                val alphaVantageResponse = response.body()
                if (alphaVantageResponse?.`Global Quote` == null) {
                    // Check if the response indicates an invalid API key
                    val errorBody = response.errorBody()?.string()
                    if (errorBody?.contains("Invalid API key") == true) {
                        return ApiResult.Error(
                            ErrorCode.API_ERROR,
                            "Invalid API key"
                        )
                    }
                    return ApiResult.Error(
                        ErrorCode.PARSE_ERROR,
                        "Invalid response format"
                    )
                }

                val priceData = mapToPriceData(alphaVantageResponse)
                // Cache the successful response
                cacheManager.cachePriceData(symbol, priceData)
                return ApiResult.Success(priceData)
            } catch (e: HttpException) {
                lastException = e
                if (attempt < MAX_RETRIES - 1) delay(RETRY_DELAY_MS)
            } catch (e: IOException) {
                lastException = e
                if (attempt < MAX_RETRIES - 1) delay(RETRY_DELAY_MS)
            } catch (e: Exception) {
                return ApiResult.Error(
                    ErrorCode.UNKNOWN_ERROR,
                    "Unexpected error: ${e.message}"
                )
            }
        }

        return ApiResult.Error(
            ErrorCode.NETWORK_ERROR,
            "Network error after $MAX_RETRIES attempts: ${lastException?.message}"
        )
    }

    private fun mapToPriceData(response: AlphaVantageResponse): PriceData {
        val quote = response.`Global Quote`!!
        return PriceData(
            symbol = quote.`01. symbol`,
            currentPrice = quote.`05. price`.toDoubleOrNull() ?: 0.0,
            openPrice = quote.`02. open`.toDoubleOrNull() ?: 0.0,
            highPrice = quote.`03. high`.toDoubleOrNull() ?: 0.0,
            lowPrice = quote.`04. low`.toDoubleOrNull() ?: 0.0,
            volume = quote.`06. volume`.toLongOrNull() ?: 0L,
            averageVolume = 0L,
            timestamp = System.currentTimeMillis()
        )
    }
}
