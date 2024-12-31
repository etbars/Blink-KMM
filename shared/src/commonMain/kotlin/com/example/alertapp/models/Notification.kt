package com.example.alertapp.models

import kotlinx.serialization.Serializable

@Serializable
data class Notification(
    val id: String = "",
    val title: String,
    val message: String,
    val timestamp: Long = 0,
    val priority: NotificationPriority = NotificationPriority.DEFAULT
)

@Serializable
enum class NotificationPriority {
    LOW, DEFAULT, HIGH
}
