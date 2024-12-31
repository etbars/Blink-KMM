package com.example.alertapp.services.processors

import com.example.alertapp.models.Alert
import com.example.alertapp.models.AlertType
import com.example.alertapp.models.AlertTrigger
import com.example.alertapp.models.ContentSource
import com.example.alertapp.services.base.*
import kotlinx.serialization.Serializable

abstract class ContentAlertProcessor : BaseAlertProcessor() {

    override val supportedType: AlertType = AlertType.CONTENT

    override suspend fun processAlert(alert: Alert): ProcessingResult {
        if (alert.trigger !is AlertTrigger.ContentTrigger) {
            return error("Invalid trigger type for content alert")
        }

        val trigger = alert.trigger as AlertTrigger.ContentTrigger
        if (trigger.sources.isEmpty()) {
            return notTriggered("No content sources specified")
        }
        if (trigger.keywords.isEmpty()) {
            return notTriggered("No keywords specified")
        }

        return try {
            val contentResult = getContent(trigger.sources)
            when (contentResult) {
                is ContentResult.Success -> {
                    processContent(trigger, contentResult.items)
                }
                is ContentResult.Error -> {
                    error(contentResult.message)
                }
            }
        } catch (e: Exception) {
            logError("Error processing content alert", e)
            error("Failed to process content: ${e.message}")
        }
    }

    override fun getConfigurationSchema(): Map<String, ConfigField> = mapOf(
        "sources" to ConfigField(
            type = ConfigFieldType.LIST,
            required = true,
            description = "Content sources to monitor",
            options = ContentSource.values().map { it.name }
        ),
        "keywords" to ConfigField(
            type = ConfigFieldType.LIST,
            required = true,
            description = "Keywords to search for in content"
        ),
        "mustIncludeAll" to ConfigField(
            type = ConfigFieldType.BOOLEAN,
            required = false,
            description = "Whether all keywords must be present",
            defaultValue = "false"
        ),
        "excludeKeywords" to ConfigField(
            type = ConfigFieldType.LIST,
            required = false,
            description = "Keywords to exclude from matches"
        )
    )

    override fun validateAlertSpecific(alert: Alert): Boolean {
        if (alert.trigger !is AlertTrigger.ContentTrigger) {
            logWarning("Invalid trigger type for alert ${alert.id}")
            return false
        }

        val trigger = alert.trigger as AlertTrigger.ContentTrigger
        if (trigger.sources.isEmpty()) {
            logWarning("No sources specified for alert ${alert.id}")
            return false
        }

        if (trigger.keywords.isEmpty()) {
            logWarning("No keywords specified for alert ${alert.id}")
            return false
        }

        return true
    }

    private suspend fun processContent(
        trigger: AlertTrigger.ContentTrigger,
        contentItems: List<ContentItem>
    ): ProcessingResult {
        val matchedItems = contentItems.filter { item ->
            matchesKeywords(item.text, trigger.keywords, trigger.mustIncludeAll, trigger.excludeKeywords)
        }

        if (matchedItems.isEmpty()) {
            return notTriggered("No matching content found")
        }

        val sentiment = analyzeSentiment(matchedItems)
        val engagementScore = calculateEngagementScore(matchedItems)

        return triggered(
            message = buildTriggeredMessage(matchedItems, trigger.keywords),
            data = mapOf(
                "matchedItems" to matchedItems.map { it.id }.joinToString(","),
                "keywords" to trigger.keywords.joinToString(","),
                "sentiment" to sentiment.name,
                "engagementScore" to engagementScore.toString()
            ),
            metadata = mapOf(
                "itemCount" to matchedItems.size.toString(),
                "sources" to trigger.sources.joinToString(",")
            )
        )
    }

    private fun buildTriggeredMessage(
        items: List<ContentItem>,
        keywords: List<String>
    ): String {
        return "Found ${items.size} items matching keywords: ${keywords.joinToString(", ")}"
    }

    protected abstract suspend fun getContent(sources: List<ContentSource>): ContentResult
    protected abstract fun matchesKeywords(
        text: String,
        keywords: List<String>,
        mustIncludeAll: Boolean,
        excludeKeywords: List<String>
    ): Boolean
    protected abstract fun analyzeSentiment(items: List<ContentItem>): Sentiment
    protected abstract fun calculateEngagementScore(items: List<ContentItem>): Double
}

@Serializable
sealed class ContentResult {
    @Serializable
    data class Success(val items: List<ContentItem>) : ContentResult()
    
    @Serializable
    data class Error(val message: String) : ContentResult()
}

@Serializable
data class ContentItem(
    val id: String,
    val text: String,
    val source: ContentSource,
    val timestamp: Long,
    val metadata: Map<String, String> = emptyMap()
)

@Serializable
enum class Sentiment {
    VERY_NEGATIVE,
    NEGATIVE,
    NEUTRAL,
    POSITIVE,
    VERY_POSITIVE
}
