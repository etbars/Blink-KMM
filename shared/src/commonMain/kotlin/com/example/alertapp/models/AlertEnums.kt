package com.example.alertapp.models

import kotlinx.serialization.Serializable

@Serializable
enum class AlertType {
    WEATHER,
    CONTENT,
    PRICE,
    RELEASE,
    EVENT,
    CUSTOM
}

@Serializable
enum class AlertOperator {
    EQUALS,
    NOT_EQUALS,
    GREATER_THAN,
    LESS_THAN,
    GREATER_THAN_OR_EQUALS,
    LESS_THAN_OR_EQUALS,
    CONTAINS,
    NOT_CONTAINS,
    STARTS_WITH,
    ENDS_WITH,
    MATCHES
}

@Serializable
enum class AlertLogic {
    AND,
    OR
}

@Serializable
enum class AlertFrequency {
    ONCE,
    HOURLY,
    DAILY,
    WEEKLY,
    MONTHLY
}

@Serializable
enum class AlertPriority {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL
}

@Serializable
enum class AlertActionType {
    NOTIFICATION,
    EMAIL,
    SMS,
    WEBHOOK,
    CUSTOM
}

@Serializable
enum class PriceOperator {
    ABOVE,
    BELOW,
    INCREASES_BY,
    DECREASES_BY
}

@Serializable
enum class NotificationPriority {
    LOW,
    DEFAULT,
    HIGH
}
