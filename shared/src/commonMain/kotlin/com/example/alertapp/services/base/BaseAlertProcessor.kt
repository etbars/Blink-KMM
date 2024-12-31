package com.example.alertapp.services.base

import com.example.alertapp.models.Alert
import com.example.alertapp.models.AlertType
import kotlinx.coroutines.flow.Flow

/**
 * Base implementation of AlertProcessor with common functionality
 */
abstract class BaseAlertProcessor : AlertProcessor {

    override fun validateAlert(alert: Alert): Boolean {
        if (alert.type != supportedType) {
            logWarning("Alert type mismatch: expected $supportedType, got ${alert.type}")
            return false
        }

        val schema = getConfigurationSchema()
        val requiredFields = schema.filter { it.value.required }.keys

        val hasAllRequiredFields = requiredFields.all { field ->
            alert.trigger.toString().contains(field)
        }

        if (!hasAllRequiredFields) {
            logWarning("Missing required fields for alert ${alert.id}")
            return false
        }

        return validateAlertSpecific(alert)
    }

    /**
     * Validate alert-specific requirements beyond the basic configuration validation
     */
    protected abstract fun validateAlertSpecific(alert: Alert): Boolean

    /**
     * Create a triggered result with the given message and data
     */
    protected fun triggered(
        message: String,
        data: Map<String, String> = emptyMap(),
        metadata: Map<String, String> = emptyMap()
    ): ProcessingResult = ProcessingResult.Triggered(message, data, metadata)

    /**
     * Create a not triggered result with the given reason
     */
    protected fun notTriggered(
        reason: String,
        metadata: Map<String, String> = emptyMap()
    ): ProcessingResult = ProcessingResult.NotTriggered(reason, metadata)

    /**
     * Create an error result with the given message and cause
     */
    protected fun error(
        message: String,
        errorType: String? = null
    ): ProcessingResult = ProcessingResult.Error(message, errorType)

    /**
     * Platform-specific logging implementation
     */
    protected abstract fun logWarning(message: String)
    protected abstract fun logError(message: String, error: Throwable? = null)
    protected abstract fun logInfo(message: String)
}

/**
 * Platform-specific notification handling
 */
expect interface NotificationHandler {
    suspend fun showNotification(title: String, message: String, priority: Int = 0)
    suspend fun cancelNotification(id: String)
}
