package com.example.alertapp.services.processors

import com.example.alertapp.api.content.ContentProvider
import com.example.alertapp.api.content.models.Sentiment
import com.example.alertapp.api.content.utils.KeywordMatcher
import com.example.alertapp.models.ContentSource
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AndroidContentAlertProcessor @Inject constructor(
    private val contentProvider: ContentProvider,
    private val keywordMatcher: KeywordMatcher
) : ContentAlertProcessor() {

    override suspend fun getContent(sources: List<ContentSource>): ContentResult {
        return try {
            val items = contentProvider.getContent(sources).map { apiItem ->
                ContentItem(
                    id = apiItem.id,
                    text = apiItem.text,
                    source = apiItem.source,
                    timestamp = apiItem.timestamp,
                    metadata = apiItem.metadata.mapValues { it.value.toString() }
                )
            }
            ContentResult.Success(items)
        } catch (e: Exception) {
            ContentResult.Error("Failed to fetch content: ${e.message}")
        }
    }

    override fun matchesKeywords(
        text: String,
        keywords: List<String>,
        mustIncludeAll: Boolean,
        excludeKeywords: List<String>
    ): Boolean {
        return keywordMatcher.matches(text, keywords, mustIncludeAll, excludeKeywords)
    }

    override fun analyzeSentiment(items: List<ContentItem>): Sentiment {
        val sentiments = items.mapNotNull { item ->
            item.metadata["sentiment"]?.let { Sentiment.valueOf(it) }
        }
        
        if (sentiments.isEmpty()) return Sentiment.NEUTRAL
        
        val averageScore = sentiments.map { 
            when (it) {
                Sentiment.VERY_NEGATIVE -> -2.0
                Sentiment.NEGATIVE -> -1.0
                Sentiment.NEUTRAL -> 0.0
                Sentiment.POSITIVE -> 1.0
                Sentiment.VERY_POSITIVE -> 2.0
            }
        }.average()

        return when {
            averageScore <= -1.5 -> Sentiment.VERY_NEGATIVE
            averageScore <= -0.5 -> Sentiment.NEGATIVE
            averageScore >= 1.5 -> Sentiment.VERY_POSITIVE
            averageScore >= 0.5 -> Sentiment.POSITIVE
            else -> Sentiment.NEUTRAL
        }
    }

    override fun calculateEngagementScore(items: List<ContentItem>): Double {
        return items.map { item ->
            val likes = item.metadata["likes"]?.toDoubleOrNull() ?: 0.0
            val shares = item.metadata["shares"]?.toDoubleOrNull() ?: 0.0
            val comments = item.metadata["comments"]?.toDoubleOrNull() ?: 0.0
            
            // Simple engagement score calculation
            (likes * 1.0 + shares * 2.0 + comments * 3.0) / 100.0
        }.average()
    }

    override fun logWarning(message: String) {
        Timber.w(message)
    }

    override fun logError(message: String, error: Throwable?) {
        if (error != null) {
            Timber.e(error, message)
        } else {
            Timber.e(message)
        }
    }

    override fun logInfo(message: String) {
        Timber.i(message)
    }
}
