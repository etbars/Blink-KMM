package com.example.alertapp.models

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
data class Release(
    val id: String,
    val title: String,
    val description: String,
    val version: String,
    val platform: String,
    val category: String? = null,
    val url: String,
    val rating: Double? = null,
    val publishedAt: Instant = Clock.System.now(),
    val createdAt: Instant = Clock.System.now(),
    val updatedAt: Instant = Clock.System.now()
)
