package com.example.alertapp.api.news

import com.example.alertapp.api.ApiConfig
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/**
 * Configuration for news-related API endpoints and settings.
 */
data class NewsConfig(
    override val apiKey: String,
    override val baseUrl: String = "https://newsapi.org/v2",
    override val timeout: Long = 30_000,
    val defaultLanguage: String = "en",
    val defaultCountry: String = "us",
    val defaultPageSize: Int = 20,
    val maxPageSize: Int = 100,
    val updateInterval: Duration = 30.seconds,
    val cacheExpiration: Duration = 5.minutes,
    val retryAttempts: Int = 3,
    val retryDelay: Duration = 1.seconds
) : ApiConfig {
    companion object {
        fun buildUrl(endpoint: String, params: Map<String, String>, apiKey: String): String {
            val baseUrl = "https://newsapi.org/v2"
            val parameters = params.toMutableMap()
            parameters["apiKey"] = apiKey
            
            val queryString = parameters.entries.joinToString("&") { (key, value) ->
                "$key=$value"
            }
            
            return "$baseUrl/$endpoint?$queryString"
        }
        
        fun getTopHeadlinesUrl(country: String? = null, category: String? = null, query: String? = null, apiKey: String): String {
            val params = mutableMapOf<String, String>()
            country?.let { params["country"] = it }
            category?.let { params["category"] = it }
            query?.let { params["q"] = it }
            return buildUrl("top-headlines", params, apiKey)
        }

        fun getEverythingUrl(query: String, apiKey: String): String =
            buildUrl("everything", mapOf("q" to query), apiKey)

        fun getSourcesUrl(category: String? = null, language: String? = null, country: String? = null, apiKey: String): String {
            val params = mutableMapOf<String, String>()
            category?.let { params["category"] = it }
            language?.let { params["language"] = it }
            country?.let { params["country"] = it }
            return buildUrl("sources", params, apiKey)
        }
    }
}
