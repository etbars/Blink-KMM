package com.example.alertapp.models

import com.example.alertapp.enums.Operator
import com.example.alertapp.enums.Sentiment
import com.example.alertapp.enums.ReleaseType
import com.example.alertapp.models.weather.WeatherCondition
import com.example.alertapp.models.weather.WeatherLocation
import com.example.alertapp.models.content.ContentFilter
import kotlinx.serialization.Serializable
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName

@Serializable
data class Alert(
    val id: String = "",
    val name: String,
    val description: String,
    val trigger: AlertTrigger,
    val actions: List<AlertAction> = listOf(),  
    val priority: Priority = Priority.DEFAULT,
    val createdAt: Instant = Clock.System.now(),
    val updatedAt: Instant = Clock.System.now(),
    val enabled: Boolean = true,
    val lastTriggeredAt: Instant? = null
)

@Serializable
sealed class AlertTrigger {
    @Serializable
    @SerialName("price")
    data class PriceTrigger(
        val asset: String,
        val operator: Operator,
        val threshold: Double,
        val timeframe: String = "1h"
    ) : AlertTrigger()

    @Serializable
    @SerialName("content")
    data class ContentTrigger(
        val query: String,
        val sources: List<String> = emptyList(),
        val authors: List<String> = emptyList(),
        val minRating: Double? = null,
        val sentiment: Sentiment? = null,
        val contentType: String? = null,
        val filter: ContentFilter? = null,
        val keywords: List<String> = emptyList(),
        val excludeKeywords: List<String> = emptyList()
    ) : AlertTrigger()

    @Serializable
    @SerialName("release")
    data class ReleaseTrigger(
        val type: String,
        val creator: String? = null,
        val minRating: Double? = null,
        val releaseType: ReleaseType? = null,
        val conditions: List<String> = emptyList()
    ) : AlertTrigger()

    @Serializable
    @SerialName("weather")
    data class WeatherTrigger(
        val location: WeatherLocation,
        val conditions: List<WeatherCondition>
    ) : AlertTrigger()

    @Serializable
    @SerialName("event")
    data class EventTrigger(
        val categories: List<String> = emptyList(),
        val locations: List<String> = emptyList(),
        val keywords: List<String> = emptyList(),
        val timeRange: EventTimeRange? = null
    ) : AlertTrigger()

    @Serializable
    @SerialName("custom")
    data class CustomTrigger(
        val description: String,
        val parameters: Map<String, String> = emptyMap()
    ) : AlertTrigger()
}

@Serializable
sealed class AlertAction {
    @Serializable
    @SerialName("notification")
    data class NotificationAction(
        val title: String,
        val message: String,
        val priority: Priority = Priority.DEFAULT,
        val type: AlertActionType = AlertActionType.NOTIFICATION
    ) : AlertAction()

    @Serializable
    @SerialName("email")
    data class EmailAction(
        val recipient: String,
        val subject: String,
        val body: String,
        val attachments: List<String> = emptyList(),
        val type: AlertActionType = AlertActionType.EMAIL,
        val config: Map<String, String> = mapOf()
    ) : AlertAction()

    @Serializable
    @SerialName("webhook")
    data class WebhookAction(
        val url: String,
        val method: String = "POST",
        val headers: Map<String, String> = mapOf(),
        val body: String? = null,
        val type: AlertActionType = AlertActionType.WEBHOOK
    ) : AlertAction()

    @Serializable
    @SerialName("sms")
    data class SmsAction(
        val phoneNumber: String,
        val body: String,
        val type: AlertActionType = AlertActionType.SMS
    ) : AlertAction()
}

@Serializable
enum class Priority {
    LOW,
    DEFAULT,
    HIGH,
    URGENT
}

@Serializable
enum class AlertActionType {
    NOTIFICATION,
    EMAIL,
    WEBHOOK,
    SMS,
    CUSTOM
}
