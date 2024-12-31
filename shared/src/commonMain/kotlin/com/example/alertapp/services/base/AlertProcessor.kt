package com.example.alertapp.services.base

import com.example.alertapp.models.Alert
import com.example.alertapp.models.AlertType
import com.example.alertapp.network.ApiResponse
import kotlinx.serialization.Serializable

@Serializable
sealed class ProcessingResult {
    @Serializable
    data class Triggered(
        val message: String,
        val data: Map<String, String> = emptyMap(), // Changed from Any to String for cross-platform compatibility
        val metadata: Map<String, String> = emptyMap()
    ) : ProcessingResult()

    @Serializable
    data class NotTriggered(
        val reason: String,
        val metadata: Map<String, String> = emptyMap()
    ) : ProcessingResult()

    @Serializable
    data class Error(
        val message: String,
        val errorType: String? = null
    ) : ProcessingResult()
}

@kotlinx.serialization.Serializable
data class ProcessingResultData(
    val triggered: Boolean,
    val reason: String,
    val data: Map<String, String> = emptyMap(),
    val timestamp: Long = System.currentTimeMillis()
)

interface AlertProcessor {
    /**
     * The type of alerts this processor can handle
     */
    val supportedType: AlertType

    /**
     * Process an alert and determine if it should be triggered
     */
    suspend fun processAlert(alert: Alert): ProcessingResult

    /**
     * Get the configuration schema for this alert type
     */
    fun getConfigurationSchema(): Map<String, ConfigurationField>

    /**
     * Validate that an alert meets the basic requirements for processing
     */
    fun validateAlert(alert: Alert): Boolean
}

@Serializable
data class ConfigurationField(
    val type: ConfigurationFieldType,
    val required: Boolean,
    val description: String,
    val options: List<String> = emptyList(),
    val defaultValue: String? = null
)

enum class ConfigurationFieldType {
    STRING,
    NUMBER,
    BOOLEAN,
    LIST,
    ENUM,
    OBJECT
}
