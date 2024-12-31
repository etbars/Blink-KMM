package com.example.alertapp.services.processors

import com.example.alertapp.models.*
import com.example.alertapp.network.ApiResponse
import com.example.alertapp.network.services.ContentApiService
import org.koin.core.component.inject

class IosContentAlertProcessor : ContentAlertProcessor() {
    private val contentApiService: ContentApiService by inject()

    override suspend fun getContent(sources: List<ContentSource>): ContentResult {
        return try {
            val response = contentApiService.getContent(sources)
            when (response) {
                is ApiResponse.Success -> {
                    ContentResult.Success(response.data)
                }
                is ApiResponse.Error -> {
                    ContentResult.Error(response.message)
                }
            }
        } catch (e: Exception) {
            ContentResult.Error("Failed to fetch content: ${e.message}")
        }
    }

    override fun matchesKeywords(text: String, keywords: List<String>, mustIncludeAll: Boolean): Boolean {
        if (keywords.isEmpty()) return true
        
        val textLower = text.lowercase()
        val keywordsLower = keywords.map { it.lowercase() }
        
        return if (mustIncludeAll) {
            keywordsLower.all { keyword -> textLower.contains(keyword) }
        } else {
            keywordsLower.any { keyword -> textLower.contains(keyword) }
        }
    }

    override fun analyzeSentiment(text: String): Sentiment {
        // Simple rule-based sentiment analysis
        // In a real app, this would use a more sophisticated ML model
        val positiveWords = setOf("good", "great", "awesome", "excellent", "happy", "positive")
        val negativeWords = setOf("bad", "terrible", "awful", "horrible", "sad", "negative")
        
        val words = text.lowercase().split(Regex("\\W+"))
        val positiveCount = words.count { it in positiveWords }
        val negativeCount = words.count { it in negativeWords }
        
        return when {
            positiveCount > negativeCount + 2 -> Sentiment.VERY_POSITIVE
            positiveCount > negativeCount -> Sentiment.POSITIVE
            negativeCount > positiveCount + 2 -> Sentiment.VERY_NEGATIVE
            negativeCount > positiveCount -> Sentiment.NEGATIVE
            else -> Sentiment.NEUTRAL
        }
    }

    override fun logWarning(message: String) {
        NSLog("⚠️ Content Alert: $message")
    }

    override fun logError(message: String, error: Throwable?) {
        if (error != null) {
            NSLog("❌ Content Alert Error: $message - ${error.message}")
        } else {
            NSLog("❌ Content Alert Error: $message")
        }
    }

    override fun logInfo(message: String) {
        NSLog("ℹ️ Content Alert: $message")
    }
}
