package com.example.alertapp.models

import kotlinx.serialization.Serializable
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

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
            type = type,
            name = name,
            description = description,
            trigger = trigger,
            sources = listOf(AlertSource(type)), // Default source based on type
            actions = emptyList(), // Actions should be configured separately
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
            // Create a basic trigger with a single condition
            return AlertTrigger(
                conditions = listOf(
                    AlertCondition(
                        field = params["field"] ?: "value",
                        operator = AlertOperator.EQUALS,
                        value = params["value"] ?: ""
                    )
                )
            )
        }

        private fun generateId(): String {
            return Clock.System.now().toEpochMilliseconds().toString()
        }
    }
}
