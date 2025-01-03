package com.example.alertapp.validation

import com.example.alertapp.models.Alert
import com.example.alertapp.models.AlertTrigger
import com.example.alertapp.models.AlertAction
import com.example.alertapp.models.ValidationResult
import com.example.alertapp.models.weather.WeatherCondition
import com.example.alertapp.models.weather.WeatherLocation
import com.example.alertapp.models.content.ContentFilter
import com.example.alertapp.enums.Operator
import com.example.alertapp.enums.Sentiment
import com.example.alertapp.enums.ReleaseType

class AlertValidator {
    fun validateAlert(alert: Alert): ValidationResult {
        val errors = mutableMapOf<String, String>()
        val warnings = mutableMapOf<String, String>()

        if (alert.name.isBlank()) {
            errors["name"] = "Alert name cannot be empty"
        }

        if (alert.description.isBlank()) {
            warnings["description"] = "Consider adding a description for better clarity"
        }

        when (val trigger = alert.trigger) {
            is AlertTrigger.PriceTrigger -> validatePriceTrigger(trigger, errors, warnings)
            is AlertTrigger.ContentTrigger -> validateContentTrigger(trigger, errors, warnings)
            is AlertTrigger.WeatherTrigger -> validateWeatherTrigger(trigger, errors, warnings)
            is AlertTrigger.ReleaseTrigger -> validateReleaseTrigger(trigger, errors, warnings)
            is AlertTrigger.EventTrigger -> validateEventTrigger(trigger, errors, warnings)
            is AlertTrigger.CustomTrigger -> validateCustomTrigger(trigger, errors, warnings)
            else -> errors["trigger"] = "Unknown trigger type"
        }

        alert.actions.forEachIndexed { index, action ->
            when (action) {
                is AlertAction.NotificationAction -> {
                    if (action.title.isBlank()) {
                        errors["actions.$index.title"] = "Notification title cannot be empty"
                    }
                    if (action.message.isBlank()) {
                        errors["actions.$index.message"] = "Notification message cannot be empty"
                    }
                }
                is AlertAction.EmailAction -> {
                    if (action.recipient.isBlank()) {
                        errors["actions.$index.recipient"] = "Email recipient cannot be empty"
                    } else if (!isValidEmail(action.recipient)) {
                        errors["actions.$index.recipient"] = "Invalid email format"
                    }
                    if (action.subject.isBlank()) {
                        errors["actions.$index.subject"] = "Email subject cannot be empty"
                    }
                    if (action.body.isBlank()) {
                        errors["actions.$index.body"] = "Email body cannot be empty"
                    }
                }
                is AlertAction.WebhookAction -> {
                    if (action.url.isBlank()) {
                        errors["actions.$index.url"] = "Webhook URL cannot be empty"
                    } else if (!isValidUrl(action.url)) {
                        errors["actions.$index.url"] = "Invalid URL format"
                    }
                }
                is AlertAction.SmsAction -> {
                    if (action.phoneNumber.isBlank()) {
                        errors["actions.$index.phoneNumber"] = "Phone number cannot be empty"
                    }
                    if (action.body.isBlank()) {
                        errors["actions.$index.body"] = "SMS body cannot be empty"
                    }
                }
                else -> errors["actions.$index"] = "Unknown action type"
            }
        }

        return ValidationResult(
            isValid = errors.isEmpty(),
            errors = errors,
            warnings = warnings
        )
    }

    private fun validatePriceTrigger(
        trigger: AlertTrigger.PriceTrigger,
        errors: MutableMap<String, String>,
        warnings: MutableMap<String, String>
    ) {
        if (trigger.asset.isBlank()) {
            errors["trigger.asset"] = "Asset cannot be empty"
        }
        if (trigger.threshold <= 0) {
            errors["trigger.threshold"] = "Threshold must be greater than 0"
        }
    }

    private fun validateContentTrigger(
        trigger: AlertTrigger.ContentTrigger,
        errors: MutableMap<String, String>,
        warnings: MutableMap<String, String>
    ) {
        if (trigger.query.isBlank()) {
            errors["trigger.query"] = "Search query cannot be empty"
        }
        if (trigger.minRating != null && (trigger.minRating < 0 || trigger.minRating > 1)) {
            errors["trigger.minRating"] = "Rating must be between 0 and 1"
        }
        if (trigger.sources.isEmpty()) {
            warnings["trigger.sources"] = "Consider specifying content sources for better results"
        }
    }

    private fun validateWeatherTrigger(
        trigger: AlertTrigger.WeatherTrigger,
        errors: MutableMap<String, String>,
        warnings: MutableMap<String, String>
    ) {
        if (trigger.location == null) {
            errors["trigger.location"] = "Weather location must be specified"
        }
        if (trigger.conditions.isEmpty()) {
            errors["trigger.conditions"] = "At least one weather condition must be specified"
        }
    }

    private fun validateReleaseTrigger(
        trigger: AlertTrigger.ReleaseTrigger,
        errors: MutableMap<String, String>,
        warnings: MutableMap<String, String>
    ) {
        if (trigger.type.isBlank()) {
            errors["trigger.type"] = "Release type cannot be empty"
        }
        if (trigger.conditions.isEmpty()) {
            warnings["trigger.conditions"] = "Consider adding conditions to filter releases"
        }
    }

    private fun validateEventTrigger(
        trigger: AlertTrigger.EventTrigger,
        errors: MutableMap<String, String>,
        warnings: MutableMap<String, String>
    ) {
        if (trigger.categories.isEmpty()) {
            warnings["trigger.categories"] = "Consider specifying event categories"
        }
        if (trigger.locations.isEmpty() && trigger.keywords.isEmpty()) {
            warnings["trigger.criteria"] = "Consider adding locations or keywords to filter events"
        }
    }

    private fun validateCustomTrigger(
        trigger: AlertTrigger.CustomTrigger,
        errors: MutableMap<String, String>,
        warnings: MutableMap<String, String>
    ) {
        if (trigger.description.isBlank()) {
            errors["trigger.description"] = "Custom trigger description cannot be empty"
        }
        if (trigger.parameters.isEmpty()) {
            warnings["trigger.parameters"] = "Consider adding parameters to customize trigger behavior"
        }
    }

    private fun isValidEmail(email: String): Boolean {
        val emailRegex = "[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}"
        return email.matches(emailRegex.toRegex())
    }

    private fun isValidUrl(url: String): Boolean {
        val urlRegex = "^(http|https)://[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}(/.*)?$"
        return url.matches(urlRegex.toRegex())
    }
}
