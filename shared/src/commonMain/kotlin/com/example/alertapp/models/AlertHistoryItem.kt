package com.example.alertapp.models

import kotlinx.serialization.Serializable
import kotlinx.datetime.Instant

@Serializable
data class AlertHistoryItem(
    val id: String = "",
    val alertId: String,
    val timestamp: Instant,
    val status: String,
    val message: String,
    val data: Map<String, String> = mapOf()
)
