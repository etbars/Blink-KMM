package com.example.alertapp.models.content

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
data class Content(
    val id: String,
    val title: String,
    val description: String,
    val url: String,
    val source: String,
    val type: String,
    val publishedAt: Instant,
    val createdAt: Instant = Instant.DISTANT_PAST,
    val updatedAt: Instant = Instant.DISTANT_PAST,
    val rating: Double? = null
)
