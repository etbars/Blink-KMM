package com.example.alertapp.models

import kotlinx.serialization.Serializable

@Serializable
data class ValidationResult(
    val isValid: Boolean,
    val errors: Map<String, String> = emptyMap(),
    val warnings: Map<String, String> = emptyMap()
)
