package com.example.alertapp.validation

import com.example.alertapp.models.*
import kotlinx.datetime.*

/**
 * Validates alerts and their components.
 */
class AlertValidator {
    fun validateAlert(alert: Alert): ValidationResult {
        val errors = mutableMapOf<String, String>()
        val warnings = mutableMapOf<String, String>()

        if (alert.name.isBlank()) {
            errors["name"] = "Alert name cannot be empty"
        }

        // Validate trigger
        val triggerResult = when (alert.trigger) {
            is AlertTrigger.PriceTrigger -> validatePriceTrigger(alert.trigger)
            is AlertTrigger.ContentTrigger -> validateContentTrigger(alert.trigger)
            is AlertTrigger.WeatherTrigger -> validateWeatherTrigger(alert.trigger)
            is AlertTrigger.ReleaseTrigger -> validateReleaseTrigger(alert.trigger)
            is AlertTrigger.EventTrigger -> validateEventTrigger(alert.trigger)
            is AlertTrigger.CustomTrigger -> validateCustomTrigger(alert.trigger)
        }

        errors.putAll(triggerResult.errors)
        warnings.putAll(triggerResult.warnings)

        // Validate actions
        if (alert.actions.isEmpty()) {
            warnings["actions"] = "No actions specified for this alert"
        } else {
            alert.actions.forEachIndexed { index, action ->
                when (action.type) {
                    AlertActionType.NOTIFICATION -> {
                        if (action.title.isBlank()) {
                            errors["actions.$index.title"] = "Notification title cannot be empty"
                        }
                        if (action.message.isBlank()) {
                            errors["actions.$index.message"] = "Notification message cannot be empty"
                        }
                    }
                    AlertActionType.EMAIL -> {
                        val recipient = action.config["recipient"] ?: ""
                        val subject = action.config["subject"] ?: ""
                        
                        if (recipient.isBlank()) {
                            errors["actions.$index.recipient"] = "Email recipient cannot be empty"
                        }
                        if (!isValidEmail(recipient)) {
                            errors["actions.$index.recipient"] = "Invalid email address"
                        }
                        if (subject.isBlank()) {
                            errors["actions.$index.subject"] = "Email subject cannot be empty"
                        }
                    }
                    else -> {}
                }
            }
        }

        return ValidationResult(
            isValid = errors.isEmpty(),
            errors = errors,
            warnings = warnings
        )
    }

    private fun validatePriceTrigger(trigger: AlertTrigger.PriceTrigger): ValidationResult {
        val errors = mutableMapOf<String, String>()
        val warnings = mutableMapOf<String, String>()
        
        if (trigger.asset.isBlank()) {
            errors["asset"] = "Asset symbol cannot be empty"
        }
        if (trigger.threshold <= 0) {
            errors["threshold"] = "Price threshold must be greater than 0"
        }
        if (!isValidTimeframe(trigger.timeframe)) {
            errors["timeframe"] = "Invalid timeframe format (e.g., 1h, 24h, 7d)"
        }
        
        return ValidationResult(
            isValid = errors.isEmpty(),
            errors = errors,
            warnings = warnings
        )
    }

    private fun validateContentTrigger(trigger: AlertTrigger.ContentTrigger): ValidationResult {
        val errors = mutableMapOf<String, String>()
        val warnings = mutableMapOf<String, String>()
        
        if (trigger.keywords.isEmpty()) {
            errors["keywords"] = "At least one keyword is required"
        }
        if (trigger.keywords.any { it.isBlank() }) {
            errors["keywords"] = "Keywords cannot be empty"
        }
        if (trigger.sources.isEmpty()) {
            errors["sources"] = "At least one content source must be selected"
        }
        if (trigger.excludeKeywords.any { it.isBlank() }) {
            errors["excludeKeywords"] = "Exclude keywords cannot be empty"
        }
        if (trigger.keywords.any { it.length < 3 }) {
            warnings["keywords"] = "Short keywords may lead to many false matches"
        }
        
        return ValidationResult(
            isValid = errors.isEmpty(),
            errors = errors,
            warnings = warnings
        )
    }

    private fun validateWeatherTrigger(trigger: AlertTrigger.WeatherTrigger): ValidationResult {
        val errors = mutableMapOf<String, String>()
        val warnings = mutableMapOf<String, String>()
        
        if (trigger.location.isBlank()) {
            errors["location"] = "Location cannot be empty"
        }
        if (trigger.conditions.isEmpty()) {
            errors["conditions"] = "At least one weather condition is required"
        }
        
        trigger.conditions.forEachIndexed { index, condition ->
            if (!isValidWeatherMetric(condition.metric)) {
                errors["conditions.$index.metric"] = "Invalid weather metric"
            }
            if (!isValidWeatherOperator(condition.operator)) {
                errors["conditions.$index.operator"] = "Invalid operator"
            }
        }
        
        return ValidationResult(
            isValid = errors.isEmpty(),
            errors = errors,
            warnings = warnings
        )
    }

    private fun validateReleaseTrigger(trigger: AlertTrigger.ReleaseTrigger): ValidationResult {
        val errors = mutableMapOf<String, String>()
        val warnings = mutableMapOf<String, String>()
        
        if (trigger.creator.isBlank()) {
            errors["creator"] = "Creator cannot be empty"
        }
        if (trigger.conditions.isEmpty()) {
            warnings["conditions"] = "No specific conditions set for release"
        }
        
        return ValidationResult(
            isValid = errors.isEmpty(),
            errors = errors,
            warnings = warnings
        )
    }

    private fun validateEventTrigger(trigger: AlertTrigger.EventTrigger): ValidationResult {
        val errors = mutableMapOf<String, String>()
        val warnings = mutableMapOf<String, String>()
        
        if (trigger.categories.isEmpty() && trigger.locations.isEmpty() && trigger.keywords.isEmpty()) {
            errors["criteria"] = "At least one search criteria (category, location, or keyword) is required"
        }
        
        trigger.timeRange?.let { range ->
            if (range.startHour < 0 || range.startHour > 23) {
                errors["timeRange.startHour"] = "Start hour must be between 0 and 23"
            }
            if (range.endHour < 0 || range.endHour > 23) {
                errors["timeRange.endHour"] = "End hour must be between 0 and 23"
            }
            if (range.startHour >= range.endHour) {
                errors["timeRange"] = "Start hour must be before end hour"
            }
            if (range.daysOfWeek.any { it < 1 || it > 7 }) {
                errors["timeRange.daysOfWeek"] = "Days must be between 1 (Sunday) and 7 (Saturday)"
            }
        }
        
        return ValidationResult(
            isValid = errors.isEmpty(),
            errors = errors,
            warnings = warnings
        )
    }

    private fun validateCustomTrigger(trigger: AlertTrigger.CustomTrigger): ValidationResult {
        val errors = mutableMapOf<String, String>()
        val warnings = mutableMapOf<String, String>()
        
        if (trigger.description.isBlank()) {
            warnings["description"] = "Description is recommended for custom triggers"
        }
        if (trigger.parameters.isEmpty()) {
            warnings["parameters"] = "No parameters specified for custom trigger"
        }
        
        return ValidationResult(
            isValid = errors.isEmpty(),
            errors = errors,
            warnings = warnings
        )
    }

    private fun isValidEmail(email: String): Boolean =
        email.matches(Regex("^[A-Za-z0-9+_.-]+@(.+)$"))

    private fun isValidTimeframe(timeframe: String): Boolean =
        timeframe.matches(Regex("^\\d+[hd]$"))

    private fun isValidWeatherMetric(metric: String): Boolean =
        metric.matches(Regex("^(temperature|humidity|pressure|wind_speed|precipitation)$"))

    private fun isValidWeatherOperator(operator: String): Boolean =
        operator.matches(Regex("^(eq|ne|gt|lt|gte|lte)$"))
}
