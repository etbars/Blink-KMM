package com.example.alertapp.models

import kotlinx.serialization.Serializable
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import com.example.alertapp.models.weather.WeatherCondition
import com.example.alertapp.models.weather.WeatherLocation
import com.example.alertapp.models.weather.WeatherConditionType
import com.example.alertapp.models.content.ContentFilter
import com.example.alertapp.enums.Operator

@Serializable
data class AlertSuggestion(
    val alertId: String = "",
    val type: AlertType,
    val name: String,
    val description: String,
    val confidence: Double,
    val trigger: AlertTrigger,
    val parameters: Map<String, String>, // Changed from Any to String for better cross-platform support
    val relatedAlerts: List<AlertSuggestion> = emptyList(),
    val requiredInputs: List<RequiredInput> = emptyList()
) {
    @Serializable
    data class RequiredInput(
        val name: String,
        val description: String,
        val type: InputType,
        val options: List<String>? = null,
        val defaultValue: String? = null,
        val validation: ValidationRule? = null
    )

    @Serializable
    enum class InputType {
        TEXT,
        NUMBER,
        BOOLEAN,
        SELECT,
        DATE,
        TIME,
        LOCATION
    }

    @Serializable
    data class ValidationRule(
        val type: ValidationType,
        val params: Map<String, String> // Changed from Any to String for better cross-platform support
    )

    @Serializable
    enum class ValidationType {
        REQUIRED,
        MIN_LENGTH,
        MAX_LENGTH,
        MIN_VALUE,
        MAX_VALUE,
        PATTERN,
        DATE_RANGE,
        CUSTOM
    }

    /**
     * Creates an Alert instance from this suggestion.
     */
    fun toAlert(): Alert {
        val now = Clock.System.now()
        return Alert(
            id = alertId.ifEmpty { generateId() },
            name = name,
            description = description,
            trigger = trigger,
            createdAt = now,
            updatedAt = now
        )
    }

    companion object {
        /**
         * Creates an AlertSuggestion from natural language input analysis.
         */
        fun fromAnalysis(
            type: AlertType,
            description: String,
            confidence: Double,
            extractedParams: Map<String, String>
        ): AlertSuggestion {
            val name = generateNameFromDescription(description)
            val trigger = generateTrigger(type, extractedParams)
            return AlertSuggestion(
                type = type,
                name = name,
                description = description,
                confidence = confidence,
                trigger = trigger,
                parameters = extractedParams
            )
        }

        private fun generateNameFromDescription(description: String): String {
            return description.take(50).let { 
                if (description.length > 50) "$it..." else it 
            }
        }

        private fun generateTrigger(type: AlertType, params: Map<String, String>): AlertTrigger {
            return when (type) {
                AlertType.PRICE -> AlertTrigger.PriceTrigger(
                    asset = params["asset"] ?: "",
                    operator = Operator.GREATER_THAN,
                    threshold = params["threshold"]?.toDoubleOrNull() ?: 0.0
                )
                AlertType.CONTENT -> AlertTrigger.ContentTrigger(
                    query = params["query"] ?: "",
                    keywords = params["keywords"]?.split(",")?.map { it.trim() } ?: emptyList()
                )
                AlertType.WEATHER -> AlertTrigger.WeatherTrigger(
                    location = WeatherLocation(
                        name = params["location"] ?: "",
                        latitude = params["latitude"]?.toDoubleOrNull() ?: 0.0,
                        longitude = params["longitude"]?.toDoubleOrNull() ?: 0.0
                    ),
                    conditions = listOf(
                        WeatherCondition(
                            type = WeatherConditionType.valueOf(params["condition_type"] ?: "TEMPERATURE"),
                            threshold = params["threshold"]?.toDoubleOrNull()
                        )
                    )
                )
                AlertType.RELEASE -> AlertTrigger.ReleaseTrigger(
                    type = params["release_type"] ?: "",
                    creator = params["creator"]
                )
                AlertType.EVENT -> AlertTrigger.EventTrigger(
                    categories = params["categories"]?.split(",")?.map { it.trim() } ?: emptyList(),
                    locations = params["locations"]?.split(",")?.map { it.trim() } ?: emptyList(),
                    keywords = params["keywords"]?.split(",")?.map { it.trim() } ?: emptyList()
                )
                AlertType.CUSTOM -> AlertTrigger.CustomTrigger(
                    description = params["description"] ?: "",
                    parameters = params
                )
            }
        }

        private fun generateId(): String {
            return Clock.System.now().toEpochMilliseconds().toString()
        }
    }
}
