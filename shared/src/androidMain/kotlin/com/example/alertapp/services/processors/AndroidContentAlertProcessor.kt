package com.example.alertapp.services.processors

import com.example.alertapp.api.ApiResponse
import com.example.alertapp.api.content.ContentProvider
import com.example.alertapp.models.content.Content
import com.example.alertapp.models.content.ContentFilter
import com.example.alertapp.models.content.ContentResult
import com.example.alertapp.models.content.ContentItem
import com.example.alertapp.enums.Sentiment
import javax.inject.Inject
import javax.inject.Singleton
import co.touchlab.kermit.Logger
import kotlinx.datetime.Clock

@Singleton
class AndroidContentAlertProcessor @Inject constructor(
    private val contentProvider: ContentProvider,
    private val logger: Logger = Logger.withTag("AndroidContentAlertProcessor")
) {
    suspend fun getContent(query: String, filter: ContentFilter): ContentResult {
        try {
            return when (val response = contentProvider.getContent(filter)) {
                is ApiResponse.Success<List<Content>> -> {
                    val contents = response.data
                    if (contents.isNotEmpty()) {
                        val items = contents.map { content ->
                            ContentItem(
                                id = content.id,
                                title = content.title,
                                text = content.description,
                                url = content.url,
                                source = content.source,
                                timestamp = content.publishedAt,
                                metadata = mapOf(
                                    "source" to content.source
                                )
                            )
                        }
                        ContentResult.Success(items)
                    } else {
                        ContentResult.Error("No content found matching the query")
                    }
                }
                is ApiResponse.Error -> {
                    logger.e(response.error.message, response.error)
                    ContentResult.Error(response.error.message)
                }
                is ApiResponse.Loading -> ContentResult.Loading
            }
        } catch (e: Exception) {
            logger.e("Error processing content", e)
            return ContentResult.Error(e.message ?: "Unknown error occurred")
        }
    }

    fun matchesKeywords(text: String, keywords: List<String>, mustIncludeAll: Boolean = false, excludeKeywords: List<String> = emptyList()): Boolean {
        if (keywords.isEmpty()) return true
        
        val textLower = text.lowercase()
        val matches = keywords.count { keyword ->
            textLower.contains(keyword.lowercase())
        }
        
        val hasExcluded = excludeKeywords.any { keyword ->
            textLower.contains(keyword.lowercase())
        }
        
        return !hasExcluded && (
            if (mustIncludeAll) matches == keywords.size
            else matches > 0
        )
    }

    fun logWarning(message: String) {
        logger.w(message)
    }

    fun logError(message: String, error: Throwable? = null) {
        if (error != null) {
            logger.e(message, error)
        } else {
            logger.e(message)
        }
    }

    fun logInfo(message: String) {
        logger.i(message)
    }

    fun analyzeSentiment(items: List<ContentItem>): Sentiment {
        val sentiments = items.mapNotNull { item ->
            item.metadata["sentiment"]?.let { sentiment ->
                runCatching { Sentiment.valueOf(sentiment.uppercase()) }.getOrNull()
            }
        }
        return when {
            sentiments.isEmpty() -> Sentiment.NEUTRAL
            sentiments.count { it == Sentiment.POSITIVE } > sentiments.size / 2 -> Sentiment.POSITIVE
            sentiments.count { it == Sentiment.NEGATIVE } > sentiments.size / 2 -> Sentiment.NEGATIVE
            else -> Sentiment.NEUTRAL
        }
    }

    fun calculateEngagement(items: List<ContentItem>): Double {
        if (items.isEmpty()) return 0.0
        
        return items.map { item ->
            val likes = item.metadata["likes"]?.toDoubleOrNull() ?: 0.0
            val shares = item.metadata["shares"]?.toDoubleOrNull() ?: 0.0
            val comments = item.metadata["comments"]?.toDoubleOrNull() ?: 0.0
            
            (likes * 1.0 + shares * 2.0 + comments * 1.5) / 4.5
        }.average()
    }
}
