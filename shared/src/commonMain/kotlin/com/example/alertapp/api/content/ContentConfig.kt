package com.example.alertapp.api.content

import com.example.alertapp.api.ApiConfig
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/**
 * Configuration for content-related API endpoints and settings.
 */
data class ContentConfig(
    override val apiKey: String,
    override val baseUrl: String = "https://api.example.com/content/v1",
    override val timeout: Long = 30_000,
    val batchSize: Int = 10,
    val updateInterval: Duration = 30.seconds,
    val cacheExpiration: Duration = 5.minutes,
    val retryAttempts: Int = 3,
    val retryDelay: Duration = 1.seconds,
    val filters: ContentFilters = ContentFilters()
) : ApiConfig

/**
 * Filters for content queries.
 */
data class ContentFilters(
    val minRelevanceScore: Double = 0.0,
    val excludedSources: Set<String> = emptySet(),
    val excludedAuthors: Set<String> = emptySet(),
    val minEngagement: Int = 0,
    val sentiments: Set<String> = emptySet(),
    val keywords: Set<String> = emptySet()
)
