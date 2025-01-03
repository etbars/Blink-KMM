package com.example.alertapp.models

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

@Serializable
sealed class ProcessingResult {
    @Serializable
    @SerialName("triggered")
    data class Triggered(
        val message: String,
        val metadata: Map<String, String> = emptyMap()
    ) : ProcessingResult()

    @Serializable
    @SerialName("not_triggered")
    data class NotTriggered(
        val message: String,
        val metadata: Map<String, String> = emptyMap()
    ) : ProcessingResult()

    @Serializable
    @SerialName("error")
    data class Error(
        val message: String,
        val code: String,
        val metadata: Map<String, String> = emptyMap()
    ) : ProcessingResult()
}
