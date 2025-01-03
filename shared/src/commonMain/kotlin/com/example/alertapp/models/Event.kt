package com.example.alertapp.models

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
data class Event(
    val id: String,
    val title: String,
    val description: String,
    val category: EventCategory,
    val startDate: Instant,
    val endDate: Instant? = null,
    val location: String? = null,
    val recurrenceRule: RecurrenceRule? = null,
    val metadata: Map<String, String> = emptyMap(),
    val createdAt: Instant,
    val updatedAt: Instant? = null
)

@Serializable
enum class EventCategory {
    MEETING,
    APPOINTMENT,
    REMINDER,
    TASK,
    OTHER
}

@Serializable
data class RecurrenceRule(
    val frequency: RecurrenceFrequency,
    val interval: Int = 1,
    val daysOfWeek: Set<DayOfWeek> = emptySet(),
    val count: Int? = null,
    val until: Instant? = null
)

@Serializable
enum class RecurrenceFrequency {
    DAILY,
    WEEKLY,
    MONTHLY,
    YEARLY
}

@Serializable
enum class DayOfWeek {
    MONDAY,
    TUESDAY,
    WEDNESDAY,
    THURSDAY,
    FRIDAY,
    SATURDAY,
    SUNDAY
}
