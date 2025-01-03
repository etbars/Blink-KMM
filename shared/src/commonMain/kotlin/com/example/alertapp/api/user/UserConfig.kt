package com.example.alertapp.api.user

import com.example.alertapp.api.ApiConfig
import kotlinx.serialization.Serializable
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/**
 * Configuration for user-related API endpoints and settings.
 */
@Serializable
enum class UserRole {
    ADMIN,
    USER,
    GUEST
}

data class UserConfig(
    override val apiKey: String,
    override val baseUrl: String = "https://api.example.com/users/v1",
    override val timeout: Long = 30_000,
    val defaultRole: UserRole = UserRole.USER,
    val updateInterval: Duration = 30.seconds,
    val cacheExpiration: Duration = 5.minutes,
    val retryAttempts: Int = 3,
    val retryDelay: Duration = 1.seconds,
    val maxConcurrentRequests: Int = 5,
    val filters: UserFilters = UserFilters()
) : ApiConfig

/**
 * Filters for user queries.
 */
data class UserFilters(
    val roles: Set<UserRole> = emptySet(),
    val statuses: Set<String> = emptySet(),
    val excludedIds: Set<String> = emptySet()
)
