package com.example.alertapp.models

import kotlinx.datetime.LocalTime
import kotlinx.serialization.Serializable

@Serializable
data class EventTimeRange(
    val startHour: Int,
    val endHour: Int,
    val daysOfWeek: List<Int> = listOf(), // 1 (Sunday) to 7 (Saturday)
    val timezone: String = "UTC"
) {
    fun isWithinRange(time: LocalTime, dayOfWeek: Int): Boolean {
        return dayOfWeek in daysOfWeek && time.hour in startHour..endHour
    }
}
