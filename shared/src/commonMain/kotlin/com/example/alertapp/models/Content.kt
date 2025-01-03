package com.example.alertapp.models

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
data class Content(
    val id: String,
    val title: String,
    val description: String? = null,
    val author: String? = null,
    val categories: List<String> = emptyList(),
    val source: String,
    val url: String? = null,
    val rating: Double? = null,
    val createdAt: Instant? = null,
    val updatedAt: Instant? = null,
    val metadata: Map<String, String> = emptyMap()
)
