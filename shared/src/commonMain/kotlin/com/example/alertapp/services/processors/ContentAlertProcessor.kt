package com.example.alertapp.services.processors

import com.example.alertapp.models.Alert
import com.example.alertapp.models.AlertTrigger
import com.example.alertapp.models.ProcessingResult
import com.example.alertapp.models.content.ContentItem
import com.example.alertapp.models.content.ContentFilter
import com.example.alertapp.enums.Sentiment
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable

abstract class ContentAlertProcessor : BaseAlertProcessor() {
    abstract suspend fun getContent(query: String, sources: List<String>): ContentResult
    abstract fun matchesKeywords(text: String, keywords: List<String>, mustIncludeAll: Boolean = false, excludeKeywords: List<String> = emptyList()): Boolean
    abstract fun analyzeSentiment(items: List<ContentItem>): Sentiment
    abstract fun calculateEngagementScore(items: List<ContentItem>): Double
    abstract fun logWarning(message: String)
    abstract fun logError(message: String, error: Throwable? = null)
    abstract fun logInfo(message: String)

    override suspend fun processAlert(alert: Alert): ProcessingResult {
        if (!validateAlert(alert)) {
            return ProcessingResult.Error("Invalid alert configuration", "VALIDATION_ERROR")
        }

        if (alert.trigger !is AlertTrigger.ContentTrigger) {
            return ProcessingResult.Error("Invalid trigger type", "VALIDATION_ERROR")
        }

        return try {
            val trigger = alert.trigger as AlertTrigger.ContentTrigger
            when (val result = getContent(trigger.query, trigger.sources)) {
                is ContentResult.Success -> {
                    val matchingItems = result.items.let { items ->
                        if (trigger.filter != null) {
                            items.filter { item ->
                                matchesKeywords(
                                    item.text,
                                    trigger.keywords,
                                    true,
                                    trigger.excludeKeywords
                                )
                            }
                        } else {
                            items
                        }
                    }

                    if (matchingItems.isEmpty()) {
                        return ProcessingResult.NotTriggered("No matching content found")
                    }

                    val sentiment = analyzeSentiment(matchingItems)
                    if (trigger.sentiment != null && sentiment.name != trigger.sentiment.name) {
                        return ProcessingResult.NotTriggered("Content sentiment does not match")
                    }

                    val engagementScore = calculateEngagementScore(matchingItems)
                    if (trigger.filter?.minEngagement != null && engagementScore < trigger.filter.minEngagement) {
                        return ProcessingResult.NotTriggered("Content engagement score too low")
                    }

                    ProcessingResult.Triggered(
                        message = "Found matching content with required sentiment and engagement",
                        metadata = mapOf(
                            "itemCount" to matchingItems.size.toString(),
                            "sentiment" to sentiment.name,
                            "engagementScore" to engagementScore.toString()
                        )
                    )
                }
                is ContentResult.Error -> {
                    logError("Failed to fetch content", result.error)
                    ProcessingResult.Error(
                        message = "Failed to fetch content: ${result.error.message ?: "Unknown error"}",
                        code = "CONTENT_ERROR"
                    )
                }
            }
        } catch (e: Exception) {
            logError("Error processing content alert", e)
            ProcessingResult.Error("Error processing content alert: ${e.message}", "PROCESSING_ERROR")
        }
    }
}

@Serializable
sealed class ContentResult {
    @Serializable
    data class Success(val items: List<ContentItem>) : ContentResult()
    
    @Serializable
    data class Error(@Contextual val error: Throwable) : ContentResult()
}
