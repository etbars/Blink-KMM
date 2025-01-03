package com.example.alertapp.models

import kotlinx.serialization.Serializable

@Serializable
data class NotSupportedError(
    override val message: String = "Operation not supported",
    val operation: String? = null,
    val context: Map<String, String> = emptyMap()
) : Exception(message)
