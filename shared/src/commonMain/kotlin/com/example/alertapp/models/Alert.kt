package com.example.alertapp.models

import kotlinx.serialization.Serializable
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

@Serializable
data class Alert(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val type: AlertType = AlertType.CUSTOM,
    val trigger: AlertTrigger = AlertTrigger.CustomTrigger(),
    val actions: List<AlertAction> = listOf(),
    val isActive: Boolean = true,
    val createdAt: Instant = Clock.System.now(),
    val lastTriggered: Instant? = null,
    val userId: String = "",
    val checkInterval: Long = 900000, // 15 minutes in milliseconds
    val metadata: Map<String, String> = emptyMap()
)

@Serializable
sealed class AlertTrigger {
    @Serializable
    data class PriceTrigger(
        val asset: String = "",
        val condition: PriceOperator = PriceOperator.ABOVE,
        val threshold: Double = 0.0,
        val timeframe: String = "1h"
    ) : AlertTrigger()

    @Serializable
    data class ContentTrigger(
        val sources: List<ContentSource> = listOf(),
        val keywords: List<String> = listOf(),
        val mustIncludeAll: Boolean = false,
        val excludeKeywords: List<String> = listOf()
    ) : AlertTrigger()

    @Serializable
    data class ReleaseTrigger(
        val creator: String = "",
        val mediaType: MediaType = MediaType.MOVIE,
        val conditions: Map<String, String> = mapOf()
    ) : AlertTrigger()

    @Serializable
    data class WeatherTrigger(
        val location: String = "",
        val conditions: List<WeatherConditionRule> = listOf()
    ) : AlertTrigger()

    @Serializable
    data class EventTrigger(
        val categories: Set<String> = setOf(),
        val locations: Set<String> = setOf(),
        val keywords: Set<String> = setOf(),
        val timeRange: EventTimeRange? = null,
        val reminderBefore: Long? = null, // Duration in milliseconds
        val excludeCancelled: Boolean = true,
        val excludeDeclined: Boolean = true
    ) : AlertTrigger()

    @Serializable
    data class CustomTrigger(
        val description: String = "",
        val parameters: Map<String, String> = mapOf()
    ) : AlertTrigger()
}

@Serializable
data class EventTimeRange(
    val startHour: Int,
    val endHour: Int,
    val daysOfWeek: Set<Int> = setOf() // 1 = Sunday, 7 = Saturday
)

@Serializable
data class AlertAction(
    val type: AlertActionType,
    val title: String = "",
    val message: String = "",
    val priority: NotificationPriority = NotificationPriority.DEFAULT,
    val config: Map<String, String> = emptyMap()
)

@Serializable
enum class AlertType {
    PRICE, CONTENT, WEATHER, RELEASE, EVENT, CUSTOM
}

@Serializable
enum class PriceOperator {
    ABOVE, BELOW, INCREASES_BY, DECREASES_BY
}

@Serializable
enum class AlertActionType {
    NOTIFICATION,
    EMAIL,
    SMS,
    WEBHOOK,
    SCRIPT
}

@Serializable
enum class NotificationPriority {
    LOW,
    DEFAULT,
    HIGH
}

@Serializable
data class WeatherConditionRule(
    val metric: String,
    val operator: String,
    val value: Double
)

@Serializable
enum class ContentSource {
    TWITTER,
    REDDIT,
    RSS,
    WEBSITE,
    CUSTOM
}

@Serializable
enum class MediaType {
    MOVIE,
    TV_SHOW,
    MUSIC,
    BOOK,
    GAME,
    PODCAST,
    COMIC_BOOK,
    EVENT
}
