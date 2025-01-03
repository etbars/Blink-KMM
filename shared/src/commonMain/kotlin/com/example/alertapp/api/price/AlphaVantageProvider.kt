package com.example.alertapp.api.price

import com.example.alertapp.api.errors.ApiError
import com.example.alertapp.api.ApiResponse
import com.example.alertapp.api.BaseApiProvider
import com.example.alertapp.models.price.PriceData
import com.example.alertapp.network.NetworkClient
import io.ktor.client.call.*
import io.ktor.http.*
import kotlinx.serialization.json.*
import co.touchlab.kermit.Logger
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

class AlphaVantageProvider(
    networkClient: NetworkClient,
    override val config: Map<String, String>,
    override val logger: Logger = Logger.withTag("AlphaVantageProvider")
) : BaseApiProvider(networkClient) {
    override val baseUrl = config["ALPHA_VANTAGE_URL"] ?: throw IllegalStateException("ALPHA_VANTAGE_URL not configured")
    private val apiKey = config["ALPHA_VANTAGE_KEY"] ?: throw IllegalStateException("ALPHA_VANTAGE_KEY not configured")

    suspend fun getStockPrice(symbol: String): ApiResponse<PriceData.Quote> {
        return get<JsonObject>(
            endpoint = "query",
            params = mapOf(
                "function" to "GLOBAL_QUOTE",
                "symbol" to symbol,
                "apikey" to apiKey
            )
        ).let { response ->
            when (response) {
                is ApiResponse.Success -> {
                    val json = response.data
                    if (json.containsKey("Note") && json["Note"]?.jsonPrimitive?.content?.contains("API call frequency") == true) {
                        logger.w("Rate limit exceeded for Alpha Vantage API")
                        ApiResponse.Error(ApiError.RateLimitExceeded("Alpha Vantage API rate limit exceeded"))
                    } else {
                        val quote = json["Global Quote"]?.jsonObject
                            ?: return ApiResponse.Error(ApiError.ParseError("Invalid response format"))

                        val currentPrice = quote["05. price"]?.jsonPrimitive?.content?.toDoubleOrNull()
                            ?: return ApiResponse.Error(ApiError.ParseError("Invalid price format"))

                        val priceChange = quote["09. change"]?.jsonPrimitive?.content?.toDoubleOrNull()
                            ?: return ApiResponse.Error(ApiError.ParseError("Invalid price change format"))

                        val percentChange = quote["10. change percent"]?.jsonPrimitive?.content?.removeSuffix("%")?.toDoubleOrNull()
                            ?: return ApiResponse.Error(ApiError.ParseError("Invalid percent change format"))

                        val volume = quote["06. volume"]?.jsonPrimitive?.content?.toLongOrNull()
                            ?: return ApiResponse.Error(ApiError.ParseError("Invalid volume format"))

                        val timestamp = quote["07. latest trading day"]?.jsonPrimitive?.content?.let { dateStr ->
                            try {
                                Instant.parse(dateStr + "T00:00:00Z")
                            } catch (e: Exception) {
                                logger.e("Failed to parse date", e)
                                null
                            }
                        } ?: Clock.System.now()

                        ApiResponse.Success(PriceData.Quote(
                            symbol = symbol,
                            currentPrice = currentPrice,
                            priceChange = priceChange,
                            percentChange = percentChange,
                            volume = volume,
                            timestamp = timestamp
                        ))
                    }
                }
                is ApiResponse.Error -> response
                is ApiResponse.Loading -> response
            }
        }
    }
}
