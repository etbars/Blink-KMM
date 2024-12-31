package com.example.alertapp.api.event

import com.example.alertapp.models.event.EventData
import com.example.alertapp.models.event.RecurrenceRule
import kotlinx.datetime.*
import kotlin.time.Duration.Companion.days

class RecurrenceCalculator {
    fun expandRecurrence(
        event: EventData.ScheduledEvent,
        timeRange: ClosedRange<Instant>
    ): List<EventData.ScheduledEvent> {
        val rule = event.recurrence ?: return listOf(event)
        val events = mutableListOf<EventData.ScheduledEvent>()
        var currentDate = event.startTime
        var count = 0

        while (currentDate <= timeRange.endInclusive && 
               (rule.count == null || count < rule.count) &&
               (rule.endDate == null || currentDate <= rule.endDate)) {
            
            if (currentDate >= timeRange.start) {
                events.add(event.copy(
                    id = "${event.id}_${count}",
                    startTime = currentDate,
                    endTime = event.endTime?.let { end ->
                        currentDate + (end - event.startTime)
                    }
                ))
            }

            currentDate = when (rule.frequency) {
                RecurrenceRule.Frequency.DAILY -> {
                    val nextDate = currentDate.plus(rule.interval.days)
                    if (rule.daysOfWeek.isNotEmpty()) {
                        findNextMatchingDay(nextDate, rule.daysOfWeek)
                    } else {
                        nextDate
                    }
                }
                RecurrenceRule.Frequency.WEEKLY -> {
                    if (rule.daysOfWeek.isNotEmpty()) {
                        findNextWeeklyDate(currentDate, rule.daysOfWeek, rule.interval)
                    } else {
                        currentDate.plus((7 * rule.interval).days)
                    }
                }
                RecurrenceRule.Frequency.MONTHLY -> {
                    val localDate = currentDate.toLocalDateTime(TimeZone.currentSystemDefault())
                    val nextMonth = localDate.month.plus(rule.interval)
                    val nextYear = localDate.year + (nextMonth.ordinal / 12)
                    val adjustedMonth = nextMonth.plus(-(nextMonth.ordinal / 12) * 12)
                    
                    LocalDateTime(
                        nextYear,
                        adjustedMonth,
                        minOf(localDate.dayOfMonth, adjustedMonth.length(Year(nextYear).isLeap)),
                        localDate.hour,
                        localDate.minute,
                        localDate.second,
                        localDate.nanosecond
                    ).toInstant(TimeZone.currentSystemDefault())
                }
                RecurrenceRule.Frequency.YEARLY -> {
                    val localDate = currentDate.toLocalDateTime(TimeZone.currentSystemDefault())
                    LocalDateTime(
                        localDate.year + rule.interval,
                        localDate.month,
                        localDate.dayOfMonth,
                        localDate.hour,
                        localDate.minute,
                        localDate.second,
                        localDate.nanosecond
                    ).toInstant(TimeZone.currentSystemDefault())
                }
            }
            count++
        }

        return events
    }

    private fun findNextMatchingDay(
        date: Instant,
        daysOfWeek: Set<DayOfWeek>
    ): Instant {
        var currentDate = date
        while (!daysOfWeek.contains(currentDate.toLocalDateTime(TimeZone.currentSystemDefault()).dayOfWeek)) {
            currentDate = currentDate.plus(1.days)
        }
        return currentDate
    }

    private fun findNextWeeklyDate(
        date: Instant,
        daysOfWeek: Set<DayOfWeek>,
        interval: Int
    ): Instant {
        val timezone = TimeZone.currentSystemDefault()
        var currentDate = date.plus(1.days)
        val startDayOfWeek = date.toLocalDateTime(timezone).dayOfWeek
        var weekCount = 0

        while (true) {
            val currentDayOfWeek = currentDate.toLocalDateTime(timezone).dayOfWeek
            if (currentDayOfWeek == startDayOfWeek) {
                weekCount++
            }

            if (weekCount < interval) {
                currentDate = currentDate.plus(1.days)
                continue
            }

            if (daysOfWeek.contains(currentDayOfWeek)) {
                return currentDate
            }

            currentDate = currentDate.plus(1.days)
        }
    }
}
