package com.example.alertapp.models.user

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.datetime.Instant

@Serializable
data class User(
    val id: String,
    val username: String,
    val email: String,
    val role: UserRole = UserRole.USER,
    val status: String = "active",
    @SerialName("created_at")
    val createdAt: Instant,
    @SerialName("updated_at")
    val updatedAt: Instant? = null,
    @SerialName("last_login")
    val lastLogin: Instant? = null,
    val preferences: UserPreferences = UserPreferences(),
    val metadata: Map<String, String> = emptyMap()
)

@Serializable
data class UserPreferences(
    @SerialName("notification_enabled")
    val notificationEnabled: Boolean = true,
    @SerialName("email_notifications")
    val emailNotifications: Boolean = true,
    @SerialName("push_notifications")
    val pushNotifications: Boolean = true,
    val theme: String = "system",
    val language: String = "en",
    val timezone: String = "UTC"
)
