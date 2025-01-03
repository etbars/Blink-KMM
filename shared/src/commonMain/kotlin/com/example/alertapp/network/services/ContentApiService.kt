package com.example.alertapp.network.services

import com.example.alertapp.api.ApiResponse
import com.example.alertapp.api.errors.ApiError
import com.example.alertapp.models.ContentSource
import com.example.alertapp.network.NetworkClient
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.HttpStatusCode
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

class ContentApiService(
    private val networkClient: NetworkClient,
    private val apiKey: String,
    private val baseUrl: String = "https://newsapi.org/v2"
) {
    suspend fun getContent(
        sources: List<ContentSource>,
        keywords: List<String>? = null,
        query: String? = null,
        fromDate: Instant? = null,
        toDate: Instant? = null
    ): ApiResponse<List<ContentItem>> {
        return try {
            val response = networkClient.client.get("$baseUrl/everything") {
                parameter("apiKey", apiKey)
                if (!query.isNullOrBlank()) {
                    parameter("q", query)
                } else if (!keywords.isNullOrEmpty()) {
                    parameter("q", keywords.joinToString(" OR "))
                }
                parameter("sources", sources.map { it.toApiSource() }.joinToString(","))
                parameter("language", "en")
                parameter("sortBy", "publishedAt")
                fromDate?.let { parameter("from", it.toString()) }
                toDate?.let { parameter("to", it.toString()) }
            }

            when (response.status) {
                HttpStatusCode.OK -> {
                    val contentResponse = response.body<NewsResponse>()
                    if (contentResponse.status == "ok") {
                        ApiResponse.Success(contentResponse.articles.map { it.toContentItem() })
                    } else {
                        ApiResponse.Error(ApiError.ServerError("API returned error status: ${contentResponse.status}"))
                    }
                }
                HttpStatusCode.Unauthorized -> ApiResponse.Error(ApiError.AuthenticationError("Invalid API key"))
                HttpStatusCode.TooManyRequests -> ApiResponse.Error(ApiError.RateLimitError("Rate limit exceeded"))
                else -> ApiResponse.Error(ApiError.ServerError("Server returned ${response.status}"))
            }
        } catch (e: Exception) {
            ApiResponse.Error(ApiError.NetworkError("Failed to fetch content: ${e.message}"))
        }
    }

    private fun ContentSource.toApiSource(): String = when (this) {
        ContentSource.NEWS -> "reuters,bbc-news,cnn"
        ContentSource.BLOG -> "medium,dev-to,hashnode"
        ContentSource.SOCIAL_MEDIA -> "twitter,facebook,linkedin"
        ContentSource.RSS -> "rss-feeds"
        ContentSource.WEBSITE -> "web-content"
    }

    @Serializable
    private data class NewsResponse(
        val status: String,
        val totalResults: Int,
        val articles: List<Article>
    )

    @Serializable
    private data class Article(
        val source: ArticleSource,
        val author: String? = null,
        val title: String,
        val description: String? = null,
        val url: String,
        val urlToImage: String? = null,
        val publishedAt: String,
        val content: String? = null
    ) {
        fun toContentItem() = ContentItem(
            id = url,
            title = title,
            description = description ?: "",
            source = source.name ?: "Unknown",
            url = url,
            imageUrl = urlToImage,
            publishedAt = try {
                Instant.parse(publishedAt)
            } catch (e: Exception) {
                Clock.System.now()
            },
            author = author
        )
    }

    @Serializable
    private data class ArticleSource(
        val id: String? = null,
        val name: String? = null
    )
}

@Serializable
data class ContentItem(
    val id: String,
    val title: String,
    val description: String,
    val source: String,
    val url: String,
    val imageUrl: String? = null,
    val publishedAt: Instant,
    val author: String? = null
)
