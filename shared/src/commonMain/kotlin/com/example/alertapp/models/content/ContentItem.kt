package com.example.alertapp.models.content

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
data class ContentItem(
    val id: String,
    val title: String,
    val text: String,
    val url: String,
    val source: String,
    val author: String? = null,
    val timestamp: Instant,
    val metadata: Map<String, String> = emptyMap()
)
