package com.example.alertapp.models

import kotlinx.serialization.Serializable

@Serializable
data class AlertSource(
    val type: AlertType,
    val name: String = type.name.lowercase(),
    val parameters: Map<String, String> = emptyMap()
)
