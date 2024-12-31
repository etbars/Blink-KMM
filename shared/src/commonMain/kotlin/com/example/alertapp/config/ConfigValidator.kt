package com.example.alertapp.config

import com.example.alertapp.validation.ValidationResult
import com.example.alertapp.validation.FieldValidation
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/**
 * Validates configuration objects to ensure they contain valid values.
 */
object ConfigValidator {
    /**
     * Validate the entire app configuration.
     */
    fun validateAppConfig(config: AlertAppConfig): ValidationResult {
        val validations = mutableListOf<FieldValidation>()

        // Validate alert check intervals
        with(config.alertCheck) {
            if (defaultInterval < minInterval) {
                validations.add(FieldValidation.Error(
                    "alertCheck.defaultInterval",
                    "Default interval cannot be less than minimum interval"
                ))
            }
            if (defaultInterval > maxInterval) {
                validations.add(FieldValidation.Error(
                    "alertCheck.defaultInterval",
                    "Default interval cannot be greater than maximum interval"
                ))
            }
            if (maxInterval < minInterval) {
                validations.add(FieldValidation.Error(
                    "alertCheck.maxInterval",
                    "Maximum interval cannot be less than minimum interval"
                ))
            }
            if (minInterval < 1.minutes) {
                validations.add(FieldValidation.Warning(
                    "alertCheck.minInterval",
                    "Very short check intervals may impact performance"
                ))
            }
        }

        // Validate API configurations
        with(config.api) {
            validations.addAll(validateApiEndpoint("newsApi", newsApi))
            validations.addAll(validateApiEndpoint("weatherApi", weatherApi))
            validations.addAll(validateApiEndpoint("priceApi", priceApi))
        }

        // Validate cache configuration
        with(config.cache) {
            if (maxSizeBytes < 1024 * 1024) { // 1MB
                validations.add(FieldValidation.Error(
                    "cache.maxSizeBytes",
                    "Cache size must be at least 1MB"
                ))
            }
            if (maxAge > 24.hours) {
                validations.add(FieldValidation.Warning(
                    "cache.maxAge",
                    "Long cache ages may result in stale data"
                ))
            }
            if (maxStale > 30.days) {
                validations.add(FieldValidation.Warning(
                    "cache.maxStale",
                    "Very long stale times may consume excessive storage"
                ))
            }
            if (timeout > 30.seconds) {
                validations.add(FieldValidation.Warning(
                    "cache.timeout",
                    "Long cache timeouts may impact user experience"
                ))
            }
        }

        // Validate database configuration
        with(config.database) {
            if (name.isBlank()) {
                validations.add(FieldValidation.Error(
                    "database.name",
                    "Database name cannot be empty"
                ))
            }
            if (version < 1) {
                validations.add(FieldValidation.Error(
                    "database.version",
                    "Database version must be positive"
                ))
            }
        }

        // Validate pagination configuration
        with(config.pagination) {
            if (defaultPageSize <= 0) {
                validations.add(FieldValidation.Error(
                    "pagination.defaultPageSize",
                    "Default page size must be positive"
                ))
            }
            if (maxPageSize < defaultPageSize) {
                validations.add(FieldValidation.Error(
                    "pagination.maxPageSize",
                    "Maximum page size cannot be less than default page size"
                ))
            }
            if (initialPage < 0) {
                validations.add(FieldValidation.Error(
                    "pagination.initialPage",
                    "Initial page cannot be negative"
                ))
            }
            if (defaultPageSize > 50) {
                validations.add(FieldValidation.Warning(
                    "pagination.defaultPageSize",
                    "Large default page sizes may impact performance"
                ))
            }
        }

        // Validate timing configuration
        with(config.timing) {
            if (animationDurationShort > 500.milliseconds) {
                validations.add(FieldValidation.Warning(
                    "timing.animationDurationShort",
                    "Short animations should be under 500ms"
                ))
            }
            if (animationDurationMedium > 1000.milliseconds) {
                validations.add(FieldValidation.Warning(
                    "timing.animationDurationMedium",
                    "Medium animations should be under 1000ms"
                ))
            }
            if (animationDurationLong > 2000.milliseconds) {
                validations.add(FieldValidation.Warning(
                    "timing.animationDurationLong",
                    "Long animations should be under 2000ms"
                ))
            }
            if (networkTimeout > 60.seconds) {
                validations.add(FieldValidation.Warning(
                    "timing.networkTimeout",
                    "Long network timeouts may impact user experience"
                ))
            }
        }

        return ValidationResult.fromFieldValidations(validations)
    }

    /**
     * Validate platform-specific configuration.
     */
    fun validatePlatformConfig(config: PlatformConfig): ValidationResult {
        val validations = mutableListOf<FieldValidation>()

        // Validate notification configuration
        with(config.notification) {
            if (channelId.isBlank()) {
                validations.add(FieldValidation.Error(
                    "notification.channelId",
                    "Channel ID cannot be empty"
                ))
            }
            if (channelName.isBlank()) {
                validations.add(FieldValidation.Error(
                    "notification.channelName",
                    "Channel name cannot be empty"
                ))
            }
        }

        // Validate permission configuration
        with(config.permissions) {
            if (locationPermissionCode < 1000) {
                validations.add(FieldValidation.Warning(
                    "permissions.locationPermissionCode",
                    "Permission codes below 1000 may conflict with system codes"
                ))
            }
            if (notificationPermissionCode < 1000) {
                validations.add(FieldValidation.Warning(
                    "permissions.notificationPermissionCode",
                    "Permission codes below 1000 may conflict with system codes"
                ))
            }
        }

        // Validate storage configuration
        with(config.storage) {
            if (preferencesName.isBlank()) {
                validations.add(FieldValidation.Error(
                    "storage.preferencesName",
                    "Preferences name cannot be empty"
                ))
            }
            if (themeModePrefKey.isBlank()) {
                validations.add(FieldValidation.Error(
                    "storage.themeModePrefKey",
                    "Theme mode preference key cannot be empty"
                ))
            }
        }

        return ValidationResult.fromFieldValidations(validations)
    }

    /**
     * Validate work-related configuration.
     */
    fun validateWorkConfig(config: WorkConfig): ValidationResult {
        val validations = mutableListOf<FieldValidation>()

        // Validate work names
        with(config.workNames) {
            if (alertCheckWork.isBlank()) {
                validations.add(FieldValidation.Error(
                    "workNames.alertCheckWork",
                    "Alert check work name cannot be empty"
                ))
            }
            if (syncWork.isBlank()) {
                validations.add(FieldValidation.Error(
                    "workNames.syncWork",
                    "Sync work name cannot be empty"
                ))
            }
        }

        // Validate intent actions
        with(config.intentActions) {
            if (!alertTriggered.startsWith("com.example.alertapp")) {
                validations.add(FieldValidation.Error(
                    "intentActions.alertTriggered",
                    "Intent action must start with app package name"
                ))
            }
            if (!alertDismissed.startsWith("com.example.alertapp")) {
                validations.add(FieldValidation.Error(
                    "intentActions.alertDismissed",
                    "Intent action must start with app package name"
                ))
            }
        }

        return ValidationResult.fromFieldValidations(validations)
    }

    /**
     * Validate an API endpoint configuration.
     */
    private fun validateApiEndpoint(
        name: String,
        config: ApiEndpointConfig
    ): List<FieldValidation> {
        val validations = mutableListOf<FieldValidation>()

        if (!config.baseUrl.startsWith("https://")) {
            validations.add(FieldValidation.Error(
                "$name.baseUrl",
                "Base URL must use HTTPS"
            ))
        }
        if (!config.baseUrl.endsWith("/")) {
            validations.add(FieldValidation.Warning(
                "$name.baseUrl",
                "Base URL should end with /"
            ))
        }
        if (config.apiKey.length < 16) {
            validations.add(FieldValidation.Warning(
                "$name.apiKey",
                "API key seems too short"
            ))
        }
        if (config.timeout < 5.seconds) {
            validations.add(FieldValidation.Warning(
                "$name.timeout",
                "Timeout less than 5 seconds may cause failures"
            ))
        }
        if (config.timeout > 30.seconds) {
            validations.add(FieldValidation.Warning(
                "$name.timeout",
                "Timeout greater than 30 seconds may impact user experience"
            ))
        }

        return validations
    }
}
