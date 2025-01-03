package com.example.alertapp.models.content

import kotlinx.serialization.Serializable
import kotlinx.datetime.Instant

@Serializable
data class Release(
    val id: String,
    val title: String,
    val description: String?,
    val creator: String,
    val isNewCreator: Boolean,
    val isHighlyAnticipated: Boolean,
    val isTrending: Boolean,
    val isAwardWinning: Boolean,
    val releaseDate: Instant,
    val metadata: Map<String, String> = emptyMap()
)
