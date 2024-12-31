package com.example.alertapp.network.services

import com.example.alertapp.models.ContentSource
import com.example.alertapp.network.ApiResponse
import com.example.alertapp.network.NetworkClient
import io.ktor.client.call.*
import io.ktor.client.request.*
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
        keywords: List<String>? = null
    ): ApiResponse<List<ContentItem>> {
        return try {
            val response = networkClient.client.get("$baseUrl/everything") {
                parameter("apiKey", apiKey)
                parameter("q", keywords?.joinToString(" OR "))
                parameter("sources", sources.map { it.toApiSource() }.joinToString(","))
                parameter("language", "en")
                parameter("sortBy", "publishedAt")
            }

            val contentResponse = response.body<NewsResponse>()
            ApiResponse.Success(contentResponse.articles.map { it.toContentItem() })
        } catch (e: Exception) {
            ApiResponse.Error(message = "Failed to fetch content: ${e.message}")
        }
    }

    private fun ContentSource.toApiSource(): String = when (this) {
        ContentSource.NEWS_API -> "reuters,bbc-news,cnn"
        ContentSource.REDDIT -> "reddit-r-all"
        ContentSource.TWITTER -> "twitter"
        ContentSource.RSS -> "rss"
        ContentSource.CUSTOM -> "custom"
    }

    @Serializable
    private data class NewsResponse(
        val status: String,
        val totalResults: Int,
        val articles: List<Article>
    )

    @Serializable
    private data class Article(
        val title: String,
        val description: String?,
        val url: String,
        val urlToImage: String?,
        val publishedAt: String,
        val content: String?,
        val author: String?,
        val source: Source
    ) {
        fun toContentItem() = ContentItem(
            title = title,
            description = description ?: "",
            url = url,
            imageUrl = urlToImage,
            publishedAt = parsePublishedAt(publishedAt),
            content = content ?: "",
            author = author ?: "",
            source = source.name ?: ""
        )

        private fun parsePublishedAt(dateStr: String): Instant {
            return try {
                // Parse ISO 8601 date string
                Instant.parse(dateStr)
            } catch (e: Exception) {
                Clock.System.now()
            }
        }
    }

    @Serializable
    private data class Source(
        val id: String?,
        val name: String?
    )
}

@Serializable
data class ContentItem(
    val title: String,
    val description: String,
    val url: String,
    val imageUrl: String?,
    val publishedAt: Instant,
    val content: String,
    val author: String,
    val source: String
)
