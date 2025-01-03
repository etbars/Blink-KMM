package com.example.alertapp.api.news

import com.example.alertapp.api.ApiResponse
import com.example.alertapp.api.BaseApiProvider
import com.example.alertapp.api.errors.ApiError
import com.example.alertapp.models.content.Content
import com.example.alertapp.network.NetworkClient
import io.ktor.client.call.*
import io.ktor.http.*
import kotlinx.serialization.json.*
import co.touchlab.kermit.Logger
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

class NewsApiProvider(
    networkClient: NetworkClient,
    override val config: Map<String, String>,
    override val logger: Logger = Logger.withTag("NewsApiProvider")
) : BaseApiProvider(networkClient) {
    
    override val baseUrl = config["NEWS_API_URL"] ?: throw IllegalStateException("NEWS_API_URL not configured")
    private val apiKey = config["NEWS_API_KEY"] ?: throw IllegalStateException("NEWS_API_KEY not configured")

    suspend fun getNews(
        query: String? = null,
        category: String? = null,
        country: String? = null,
        language: String? = null,
        page: Int = 1,
        pageSize: Int = 20
    ): ApiResponse<List<Content>> = get<JsonObject>(
        endpoint = "top-headlines",
        params = buildMap {
            put("apiKey", apiKey)
            put("page", page.toString())
            put("pageSize", pageSize.toString())
            query?.let { put("q", it) }
            category?.let { put("category", it) }
            country?.let { put("country", it) }
            language?.let { put("language", it) }
        }
    ).let { response ->
        when (response) {
            is ApiResponse.Success -> {
                val articles = response.data["articles"]?.jsonArray?.mapNotNull { articleJson ->
                    try {
                        Content(
                            id = articleJson.jsonObject["url"]?.jsonPrimitive?.content ?: return@mapNotNull null,
                            title = articleJson.jsonObject["title"]?.jsonPrimitive?.content ?: "",
                            description = articleJson.jsonObject["description"]?.jsonPrimitive?.content ?: "",
                            url = articleJson.jsonObject["url"]?.jsonPrimitive?.content ?: "",
                            source = articleJson.jsonObject["source"]?.jsonObject?.get("name")?.jsonPrimitive?.content ?: "unknown",
                            type = "news",
                            publishedAt = articleJson.jsonObject["publishedAt"]?.jsonPrimitive?.content?.let { 
                                Instant.parse(it)
                            } ?: Clock.System.now(),
                            createdAt = Clock.System.now(),
                            updatedAt = Clock.System.now()
                        )
                    } catch (e: Exception) {
                        logger.e("Failed to parse news article", e)
                        null
                    }
                } ?: emptyList()
                ApiResponse.Success(articles)
            }
            is ApiResponse.Error -> response
            is ApiResponse.Loading -> response
        }
    }

    suspend fun searchNews(
        query: String,
        sources: List<String>? = null,
        domains: List<String>? = null,
        from: Instant? = null,
        to: Instant? = null,
        language: String? = null,
        sortBy: String? = null,
        page: Int = 1,
        pageSize: Int = 20
    ): ApiResponse<List<Content>> = get<JsonObject>(
        endpoint = "everything",
        params = buildMap {
            put("apiKey", apiKey)
            put("q", query)
            put("page", page.toString())
            put("pageSize", pageSize.toString())
            sources?.let { put("sources", it.joinToString(",")) }
            domains?.let { put("domains", it.joinToString(",")) }
            from?.let { put("from", it.toString()) }
            to?.let { put("to", it.toString()) }
            language?.let { put("language", it) }
            sortBy?.let { put("sortBy", it) }
        }
    ).let { response ->
        when (response) {
            is ApiResponse.Success -> {
                val articles = response.data["articles"]?.jsonArray?.mapNotNull { articleJson ->
                    try {
                        Content(
                            id = articleJson.jsonObject["url"]?.jsonPrimitive?.content ?: return@mapNotNull null,
                            title = articleJson.jsonObject["title"]?.jsonPrimitive?.content ?: "",
                            description = articleJson.jsonObject["description"]?.jsonPrimitive?.content ?: "",
                            url = articleJson.jsonObject["url"]?.jsonPrimitive?.content ?: "",
                            source = articleJson.jsonObject["source"]?.jsonObject?.get("name")?.jsonPrimitive?.content ?: "unknown",
                            type = "news",
                            publishedAt = articleJson.jsonObject["publishedAt"]?.jsonPrimitive?.content?.let { 
                                Instant.parse(it)
                            } ?: Clock.System.now(),
                            createdAt = Clock.System.now(),
                            updatedAt = Clock.System.now()
                        )
                    } catch (e: Exception) {
                        logger.e("Failed to parse news article", e)
                        null
                    }
                } ?: emptyList()
                ApiResponse.Success(articles)
            }
            is ApiResponse.Error -> response
            is ApiResponse.Loading -> response
        }
    }
}
