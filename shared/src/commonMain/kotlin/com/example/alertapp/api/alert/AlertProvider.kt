package com.example.alertapp.api.alert

import com.example.alertapp.api.ApiError
import com.example.alertapp.api.ApiResponse
import com.example.alertapp.api.BaseApiProvider
import com.example.alertapp.models.*
import kotlinx.serialization.json.*
import kotlinx.datetime.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import co.touchlab.kermit.Logger
import kotlin.time.Duration.Companion.milliseconds

class AlertProvider(
    override val config: AlertConfig
) : BaseApiProvider<AlertConfig>() {
    private val logger = Logger.withTag("AlertProvider")
    private val cache = AlertCache()

    suspend fun createAlert(alert: Alert): Flow<ApiResponse<Alert>> = flow {
        try {
            logger.d { "Creating alert: ${alert.name}" }
            
            val params = mapOf(
                "userId" to alert.userId
            )
            
            post<JsonObject>("alerts", params, alert).collect { response ->
                when (response) {
                    is ApiResponse.Success -> {
                        val createdAlert = parseAlert(response.data)
                        cache.cacheAlert(createdAlert)
                        emit(ApiResponse.Success(createdAlert))
                    }
                    is ApiResponse.Loading -> emit(ApiResponse.Loading)
                    is ApiResponse.Error -> emit(response)
                }
            }
        } catch (e: Exception) {
            logger.e(e) { "Failed to create alert" }
            emit(ApiResponse.Error(
                message = "Failed to create alert: ${e.message}",
                cause = e
            ))
        }
    }

    suspend fun updateAlert(alert: Alert): Flow<ApiResponse<Alert>> = flow {
        try {
            logger.d { "Updating alert: ${alert.id}" }
            
            val params = mapOf(
                "userId" to alert.userId,
                "alertId" to alert.id
            )
            
            put<JsonObject>("alerts/${alert.id}", params, alert).collect { response ->
                when (response) {
                    is ApiResponse.Success -> {
                        val updatedAlert = parseAlert(response.data)
                        cache.cacheAlert(updatedAlert)
                        emit(ApiResponse.Success(updatedAlert))
                    }
                    is ApiResponse.Loading -> emit(ApiResponse.Loading)
                    is ApiResponse.Error -> emit(response)
                }
            }
        } catch (e: Exception) {
            logger.e(e) { "Failed to update alert" }
            emit(ApiResponse.Error(
                message = "Failed to update alert: ${e.message}",
                cause = e
            ))
        }
    }

    suspend fun deleteAlert(alertId: String, userId: String): Flow<ApiResponse<Boolean>> = flow {
        try {
            logger.d { "Deleting alert: $alertId" }
            
            val params = mapOf(
                "userId" to userId,
                "alertId" to alertId
            )
            
            delete<JsonObject>("alerts/$alertId", params).collect { response ->
                when (response) {
                    is ApiResponse.Success -> {
                        cache.removeAlert(alertId)
                        emit(ApiResponse.Success(true))
                    }
                    is ApiResponse.Loading -> emit(ApiResponse.Loading)
                    is ApiResponse.Error -> emit(response)
                }
            }
        } catch (e: Exception) {
            logger.e(e) { "Failed to delete alert" }
            emit(ApiResponse.Error(
                message = "Failed to delete alert: ${e.message}",
                cause = e
            ))
        }
    }

    suspend fun getAlert(alertId: String, userId: String): Flow<ApiResponse<Alert>> = flow {
        try {
            logger.d { "Fetching alert: $alertId" }
            
            // Check cache first
            cache.getAlert(alertId)?.let { alert ->
                logger.d { "Returning cached alert" }
                emit(ApiResponse.Success(alert))
                return@flow
            }
            
            val params = mapOf(
                "userId" to userId,
                "alertId" to alertId
            )
            
            get<JsonObject>("alerts/$alertId", params).collect { response ->
                when (response) {
                    is ApiResponse.Success -> {
                        val alert = parseAlert(response.data)
                        cache.cacheAlert(alert)
                        emit(ApiResponse.Success(alert))
                    }
                    is ApiResponse.Loading -> emit(ApiResponse.Loading)
                    is ApiResponse.Error -> emit(response)
                }
            }
        } catch (e: Exception) {
            logger.e(e) { "Failed to get alert" }
            emit(ApiResponse.Error(
                message = "Failed to get alert: ${e.message}",
                cause = e
            ))
        }
    }

    suspend fun getUserAlerts(userId: String): Flow<ApiResponse<List<Alert>>> = flow {
        try {
            logger.d { "Fetching alerts for user: $userId" }
            
            // Check cache first
            cache.getUserAlerts(userId)?.let { alerts ->
                logger.d { "Returning ${alerts.size} cached alerts" }
                emit(ApiResponse.Success(filterAlerts(alerts)))
                return@flow
            }
            
            val params = mapOf("userId" to userId)
            
            get<JsonObject>("alerts", params).collect { response ->
                when (response) {
                    is ApiResponse.Success -> {
                        val alerts = parseAlertList(response.data)
                        cache.cacheUserAlerts(userId, alerts)
                        emit(ApiResponse.Success(filterAlerts(alerts)))
                    }
                    is ApiResponse.Loading -> emit(ApiResponse.Loading)
                    is ApiResponse.Error -> emit(response)
                }
            }
        } catch (e: Exception) {
            logger.e(e) { "Failed to get user alerts" }
            emit(ApiResponse.Error(
                message = "Failed to get user alerts: ${e.message}",
                cause = e
            ))
        }
    }

    private fun parseAlertList(json: JsonObject): List<Alert> {
        return json["alerts"]?.jsonArray?.mapNotNull { alertJson ->
            try {
                parseAlert(alertJson.jsonObject)
            } catch (e: Exception) {
                logger.w(e) { "Failed to parse alert" }
                null
            }
        } ?: emptyList()
    }

    private fun parseAlert(json: JsonObject): Alert {
        return Alert(
            id = json["id"]?.jsonPrimitive?.content 
                ?: throw ApiError.ParseError("Missing alert ID"),
            name = json["name"]?.jsonPrimitive?.content 
                ?: throw ApiError.ParseError("Missing alert name"),
            description = json["description"]?.jsonPrimitive?.contentOrNull ?: "",
            type = json["type"]?.jsonPrimitive?.content?.let {
                try {
                    AlertType.valueOf(it)
                } catch (e: Exception) {
                    throw ApiError.ParseError("Invalid alert type")
                }
            } ?: throw ApiError.ParseError("Missing alert type"),
            trigger = json["trigger"]?.jsonObject?.let { parseTrigger(it) }
                ?: throw ApiError.ParseError("Missing alert trigger"),
            actions = json["actions"]?.jsonArray?.mapNotNull { 
                try {
                    parseAction(it.jsonObject)
                } catch (e: Exception) {
                    logger.w(e) { "Failed to parse action" }
                    null
                }
            } ?: emptyList(),
            isActive = json["isActive"]?.jsonPrimitive?.boolean ?: true,
            createdAt = json["createdAt"]?.jsonPrimitive?.content?.let {
                try {
                    Instant.parse(it)
                } catch (e: Exception) {
                    throw ApiError.ParseError("Invalid created date format")
                }
            } ?: Clock.System.now(),
            lastTriggered = json["lastTriggered"]?.jsonPrimitive?.content?.let {
                try {
                    Instant.parse(it)
                } catch (e: Exception) {
                    logger.w(e) { "Invalid last triggered date format" }
                    null
                }
            },
            userId = json["userId"]?.jsonPrimitive?.content 
                ?: throw ApiError.ParseError("Missing user ID"),
            checkInterval = json["checkInterval"]?.jsonPrimitive?.long 
                ?: config.defaultCheckInterval.inWholeMilliseconds,
            metadata = json["metadata"]?.jsonObject?.let { parseMetadata(it) } ?: emptyMap()
        )
    }

    private fun parseTrigger(json: JsonObject): AlertTrigger {
        return when (val type = json["type"]?.jsonPrimitive?.content) {
            "price" -> parsePriceTrigger(json)
            "content" -> parseContentTrigger(json)
            "release" -> parseReleaseTrigger(json)
            "weather" -> parseWeatherTrigger(json)
            "event" -> parseEventTrigger(json)
            "custom" -> parseCustomTrigger(json)
            else -> throw ApiError.ParseError("Unknown trigger type: $type")
        }
    }

    private fun parsePriceTrigger(json: JsonObject): AlertTrigger.PriceTrigger {
        return AlertTrigger.PriceTrigger(
            asset = json["asset"]?.jsonPrimitive?.content 
                ?: throw ApiError.ParseError("Missing asset"),
            condition = json["condition"]?.jsonPrimitive?.content?.let {
                try {
                    PriceOperator.valueOf(it)
                } catch (e: Exception) {
                    throw ApiError.ParseError("Invalid price operator")
                }
            } ?: throw ApiError.ParseError("Missing price condition"),
            threshold = json["threshold"]?.jsonPrimitive?.double 
                ?: throw ApiError.ParseError("Missing threshold"),
            timeframe = json["timeframe"]?.jsonPrimitive?.content ?: "1h"
        )
    }

    private fun parseContentTrigger(json: JsonObject): AlertTrigger.ContentTrigger {
        return AlertTrigger.ContentTrigger(
            sources = json["sources"]?.jsonArray?.mapNotNull {
                try {
                    ContentSource.valueOf(it.jsonPrimitive.content)
                } catch (e: Exception) {
                    logger.w(e) { "Invalid content source" }
                    null
                }
            } ?: emptyList(),
            keywords = json["keywords"]?.jsonArray?.mapNotNull { 
                it.jsonPrimitive.contentOrNull 
            } ?: emptyList(),
            mustIncludeAll = json["mustIncludeAll"]?.jsonPrimitive?.boolean ?: false,
            excludeKeywords = json["excludeKeywords"]?.jsonArray?.mapNotNull { 
                it.jsonPrimitive.contentOrNull 
            } ?: emptyList()
        )
    }

    private fun parseReleaseTrigger(json: JsonObject): AlertTrigger.ReleaseTrigger {
        return AlertTrigger.ReleaseTrigger(
            creator = json["creator"]?.jsonPrimitive?.content ?: "",
            mediaType = json["mediaType"]?.jsonPrimitive?.content?.let {
                try {
                    MediaType.valueOf(it)
                } catch (e: Exception) {
                    throw ApiError.ParseError("Invalid media type")
                }
            } ?: MediaType.MOVIE,
            conditions = json["conditions"]?.jsonObject?.let { parseMetadata(it) } ?: emptyMap()
        )
    }

    private fun parseWeatherTrigger(json: JsonObject): AlertTrigger.WeatherTrigger {
        return AlertTrigger.WeatherTrigger(
            location = json["location"]?.jsonPrimitive?.content 
                ?: throw ApiError.ParseError("Missing location"),
            conditions = json["conditions"]?.jsonArray?.mapNotNull {
                try {
                    parseWeatherCondition(it.jsonObject)
                } catch (e: Exception) {
                    logger.w(e) { "Failed to parse weather condition" }
                    null
                }
            } ?: emptyList()
        )
    }

    private fun parseWeatherCondition(json: JsonObject): WeatherConditionRule {
        return WeatherConditionRule(
            metric = json["metric"]?.jsonPrimitive?.content 
                ?: throw ApiError.ParseError("Missing weather metric"),
            operator = json["operator"]?.jsonPrimitive?.content 
                ?: throw ApiError.ParseError("Missing weather operator"),
            value = json["value"]?.jsonPrimitive?.double 
                ?: throw ApiError.ParseError("Missing weather value")
        )
    }

    private fun parseEventTrigger(json: JsonObject): AlertTrigger.EventTrigger {
        return AlertTrigger.EventTrigger(
            categories = json["categories"]?.jsonArray?.mapNotNull { 
                it.jsonPrimitive.contentOrNull 
            }?.toSet() ?: emptySet(),
            locations = json["locations"]?.jsonArray?.mapNotNull { 
                it.jsonPrimitive.contentOrNull 
            }?.toSet() ?: emptySet(),
            keywords = json["keywords"]?.jsonArray?.mapNotNull { 
                it.jsonPrimitive.contentOrNull 
            }?.toSet() ?: emptySet(),
            timeRange = json["timeRange"]?.jsonObject?.let { parseEventTimeRange(it) },
            reminderBefore = json["reminderBefore"]?.jsonPrimitive?.long,
            excludeCancelled = json["excludeCancelled"]?.jsonPrimitive?.boolean ?: true,
            excludeDeclined = json["excludeDeclined"]?.jsonPrimitive?.boolean ?: true
        )
    }

    private fun parseEventTimeRange(json: JsonObject): EventTimeRange {
        return EventTimeRange(
            startHour = json["startHour"]?.jsonPrimitive?.int 
                ?: throw ApiError.ParseError("Missing start hour"),
            endHour = json["endHour"]?.jsonPrimitive?.int 
                ?: throw ApiError.ParseError("Missing end hour"),
            daysOfWeek = json["daysOfWeek"]?.jsonArray?.mapNotNull { 
                it.jsonPrimitive.intOrNull 
            }?.toSet() ?: emptySet()
        )
    }

    private fun parseCustomTrigger(json: JsonObject): AlertTrigger.CustomTrigger {
        return AlertTrigger.CustomTrigger(
            description = json["description"]?.jsonPrimitive?.contentOrNull ?: "",
            parameters = json["parameters"]?.jsonObject?.let { parseMetadata(it) } ?: emptyMap()
        )
    }

    private fun parseAction(json: JsonObject): AlertAction {
        return AlertAction(
            type = json["type"]?.jsonPrimitive?.content?.let {
                try {
                    AlertActionType.valueOf(it)
                } catch (e: Exception) {
                    throw ApiError.ParseError("Invalid action type")
                }
            } ?: throw ApiError.ParseError("Missing action type"),
            title = json["title"]?.jsonPrimitive?.content ?: "",
            message = json["message"]?.jsonPrimitive?.content ?: "",
            priority = json["priority"]?.jsonPrimitive?.content?.let {
                try {
                    NotificationPriority.valueOf(it)
                } catch (e: Exception) {
                    NotificationPriority.DEFAULT
                }
            } ?: NotificationPriority.DEFAULT,
            config = json["config"]?.jsonObject?.let { parseMetadata(it) } ?: emptyMap()
        )
    }

    private fun parseMetadata(json: JsonObject): Map<String, String> {
        return buildMap {
            json.entries.forEach { (key, value) ->
                when (value) {
                    is JsonPrimitive -> put(key, value.content)
                    is JsonObject, is JsonArray -> put(key, value.toString())
                }
            }
        }
    }

    private fun filterAlerts(alerts: List<Alert>): List<Alert> {
        val now = Clock.System.now()
        return alerts.filter { alert ->
            with(config.filters) {
                (!excludeInactive || alert.isActive) &&
                (types.isEmpty() || types.contains(alert.type.name)) &&
                (!excludeExpired || alert.lastTriggered?.let { lastTriggered ->
                    val age = now - lastTriggered
                    minLastTriggered?.let { min -> age >= min } ?: true &&
                    maxLastTriggered?.let { max -> age <= max } ?: true
                } ?: true)
            }
        }
    }

    private class AlertCache {
        private val alertCache = mutableMapOf<String, CacheEntry<Alert>>()
        private val userAlertsCache = mutableMapOf<String, CacheEntry<List<Alert>>>()

        fun getAlert(alertId: String): Alert? {
            val entry = alertCache[alertId] ?: return null
            if (entry.isExpired()) {
                alertCache.remove(alertId)
                return null
            }
            return entry.data
        }

        fun getUserAlerts(userId: String): List<Alert>? {
            val entry = userAlertsCache[userId] ?: return null
            if (entry.isExpired()) {
                userAlertsCache.remove(userId)
                return null
            }
            return entry.data
        }

        fun cacheAlert(alert: Alert) {
            alertCache[alert.id] = CacheEntry(alert)
        }

        fun cacheUserAlerts(userId: String, alerts: List<Alert>) {
            userAlertsCache[userId] = CacheEntry(alerts)
        }

        fun removeAlert(alertId: String) {
            alertCache.remove(alertId)
            userAlertsCache.values.forEach { entry ->
                entry.data.find { it.id == alertId }?.let { alert ->
                    userAlertsCache[alert.userId] = CacheEntry(
                        entry.data.filter { it.id != alertId }
                    )
                }
            }
        }

        private data class CacheEntry<T>(
            val data: T,
            val timestamp: Instant = Clock.System.now()
        ) {
            fun isExpired(): Boolean =
                Clock.System.now() - timestamp > 5.minutes
        }
    }
}
