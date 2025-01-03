package com.example.alertapp.models.content

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
data class ContentFilter(
    val query: String? = null,
    val category: String? = null,
    val source: String? = null,
    val fromDate: Instant? = null,
    val toDate: Instant? = null,
    val keywords: List<String> = emptyList(),
    val mustIncludeAllKeywords: Boolean = false,
    val excludeKeywords: List<String> = emptyList(),
    val minEngagement: Double? = null,
    val maxAge: Long? = null, // in seconds
    val languages: List<String> = emptyList()
)
