package com.example.alertapp.models

import kotlinx.serialization.Serializable

@Serializable
enum class NotificationPriority {
    LOW,
    DEFAULT,
    HIGH,
    URGENT
}
