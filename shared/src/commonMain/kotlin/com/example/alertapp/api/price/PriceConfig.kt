package com.example.alertapp.api.price

import com.example.alertapp.api.ApiConfig
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/**
 * Configuration for price-related API endpoints and settings.
 */
data class PriceConfig(
    override val apiKey: String,
    override val baseUrl: String = "https://www.alphavantage.co/query",
    override val timeout: Long = 30_000,
    val defaultCurrency: String = "USD",
    val updateInterval: Duration = 30.seconds,
    val cacheExpiration: Duration = 5.minutes,
    val retryAttempts: Int = 3,
    val retryDelay: Duration = 1.seconds
) : ApiConfig {
    companion object {
        fun buildUrl(function: String, params: Map<String, String>, apiKey: String): String {
            val baseUrl = "https://www.alphavantage.co/query"
            val parameters = params.toMutableMap()
            parameters["function"] = function
            parameters["apikey"] = apiKey
            
            val queryString = parameters.entries.joinToString("&") { (key, value) ->
                "$key=$value"
            }
            
            return "$baseUrl?$queryString"
        }
        
        fun getIntradayUrl(symbol: String, interval: String = "5min", apiKey: String): String =
            buildUrl("TIME_SERIES_INTRADAY", mapOf("symbol" to symbol, "interval" to interval), apiKey)

        fun getDailyUrl(symbol: String, apiKey: String): String =
            buildUrl("TIME_SERIES_DAILY", mapOf("symbol" to symbol), apiKey)

        fun getWeeklyUrl(symbol: String, apiKey: String): String =
            buildUrl("TIME_SERIES_WEEKLY", mapOf("symbol" to symbol), apiKey)

        fun getMonthlyUrl(symbol: String, apiKey: String): String =
            buildUrl("TIME_SERIES_MONTHLY", mapOf("symbol" to symbol), apiKey)

        fun getSearchUrl(keywords: String, apiKey: String): String =
            buildUrl("SYMBOL_SEARCH", mapOf("keywords" to keywords), apiKey)

        fun getQuoteUrl(symbol: String, apiKey: String): String =
            buildUrl("GLOBAL_QUOTE", mapOf("symbol" to symbol), apiKey)
    }
}
