package com.example.alertapp.nlp

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*
import kotlinx.datetime.Instant
import kotlinx.datetime.Clock
import co.touchlab.kermit.Logger

@Serializable
data class AlertContext(
    val timestamp: Instant = Clock.System.now(),
    val userId: String,
    val lastInput: String = "",
    val patterns: List<AlertPattern> = emptyList(),
    val metadata: Map<String, String> = emptyMap()
) {
    private val logger = Logger.withTag("AlertContext")

    fun toJson(): String = try {
        buildJsonObject {
            put("timestamp", timestamp.toString())
            put("userId", userId)
            put("lastInput", lastInput)
            put("patterns", Json.encodeToJsonElement(patterns))
            put("metadata", buildJsonObject {
                metadata.forEach { (key, value) -> put(key, value) }
            })
        }.toString()
    } catch (e: Exception) {
        logger.e(e) { "Failed to serialize AlertContext to JSON" }
        throw e
    }

    companion object {
        private val logger = Logger.withTag("AlertContext")

        fun create(userId: String): AlertContext = AlertContext(
            userId = userId,
            timestamp = Clock.System.now()
        )

        fun withPattern(
            userId: String,
            pattern: AlertPattern
        ): AlertContext = AlertContext(
            userId = userId,
            timestamp = Clock.System.now(),
            patterns = listOf(pattern)
        )

        fun withPatterns(
            userId: String,
            patterns: List<AlertPattern>
        ): AlertContext = AlertContext(
            userId = userId,
            timestamp = Clock.System.now(),
            patterns = patterns
        )

        fun fromJson(json: String): AlertContext = try {
            Json.decodeFromString(serializer(), json)
        } catch (e: Exception) {
            logger.e(e) { "Failed to deserialize AlertContext from JSON: $json" }
            throw e
        }
    }
}

@Serializable
data class AlertPattern(
    val type: String,
    val value: String,
    val pattern: String = "",
    val frequency: Int = 0,
    val lastOccurrence: Instant = Clock.System.now(),
    val confidence: Double = 1.0,
    val metadata: Map<String, String> = emptyMap()
) {
    private val logger = Logger.withTag("AlertPattern")

    fun toJsonObject() = try {
        buildJsonObject {
            put("type", type)
            put("value", value)
            put("pattern", pattern)
            put("frequency", frequency)
            put("lastOccurrence", lastOccurrence.toString())
            put("confidence", confidence)
            put("metadata", buildJsonObject {
                metadata.forEach { (key, value) -> put(key, value) }
            })
        }
    } catch (e: Exception) {
        logger.e(e) { "Failed to serialize AlertPattern to JSON" }
        throw e
    }

    companion object {
        private val logger = Logger.withTag("AlertPattern")

        const val TYPE_KEYWORD = "keyword"
        const val TYPE_ENTITY = "entity"
        const val TYPE_SENTIMENT = "sentiment"
        const val TYPE_TOPIC = "topic"
        const val TYPE_CATEGORY = "category"
        const val TYPE_CUSTOM = "custom"

        fun fromJsonObject(json: JsonObject): AlertPattern = try {
            Json.decodeFromJsonElement(serializer(), json)
        } catch (e: Exception) {
            logger.e(e) { "Failed to deserialize AlertPattern from JSON: $json" }
            throw e
        }
    }
}

@Serializable
data class TemporalRelation(
    val targetAlertId: String,
    val relation: String,
    val timeValue: Long,
    val timeUnit: String
)
