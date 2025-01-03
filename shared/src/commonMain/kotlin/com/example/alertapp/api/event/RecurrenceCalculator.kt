package com.example.alertapp.api.event

import com.example.alertapp.models.Event
import com.example.alertapp.models.RecurrenceRule
import com.example.alertapp.models.RecurrenceFrequency
import com.example.alertapp.models.DayOfWeek
import kotlinx.datetime.*

class RecurrenceCalculator {
    fun calculateNextOccurrence(event: Event): Instant? {
        val rule = event.recurrenceRule ?: return null
        val now = Clock.System.now()
        
        return when (rule.frequency) {
            RecurrenceFrequency.DAILY -> calculateNextDaily(event.startDate, now, rule)
            RecurrenceFrequency.WEEKLY -> calculateNextWeekly(event.startDate, now, rule)
            RecurrenceFrequency.MONTHLY -> calculateNextMonthly(event.startDate, now, rule)
            RecurrenceFrequency.YEARLY -> calculateNextYearly(event.startDate, now, rule)
        }
    }

    private fun calculateNextDaily(startDate: Instant, now: Instant, rule: RecurrenceRule): Instant {
        var next = startDate
        while (next <= now) {
            next = next.plus(DateTimePeriod(days = rule.interval), TimeZone.currentSystemDefault())
        }
        return next
    }

    private fun calculateNextWeekly(startDate: Instant, now: Instant, rule: RecurrenceRule): Instant {
        if (rule.daysOfWeek.isEmpty()) {
            var next = startDate
            while (next <= now) {
                next = next.plus(DateTimePeriod(days = 7 * rule.interval), TimeZone.currentSystemDefault())
            }
            return next
        }

        var candidateDate = now
        val timeZone = TimeZone.currentSystemDefault()
        while (true) {
            candidateDate = candidateDate.plus(DateTimePeriod(days = 1), timeZone)
            val dayOfWeek = candidateDate.toLocalDateTime(timeZone).dayOfWeek
            val modelDayOfWeek = when (dayOfWeek) {
                kotlinx.datetime.DayOfWeek.MONDAY -> DayOfWeek.MONDAY
                kotlinx.datetime.DayOfWeek.TUESDAY -> DayOfWeek.TUESDAY
                kotlinx.datetime.DayOfWeek.WEDNESDAY -> DayOfWeek.WEDNESDAY
                kotlinx.datetime.DayOfWeek.THURSDAY -> DayOfWeek.THURSDAY
                kotlinx.datetime.DayOfWeek.FRIDAY -> DayOfWeek.FRIDAY
                kotlinx.datetime.DayOfWeek.SATURDAY -> DayOfWeek.SATURDAY
                kotlinx.datetime.DayOfWeek.SUNDAY -> DayOfWeek.SUNDAY
                else -> DayOfWeek.MONDAY // Default to Monday if an unknown day is encountered
            }
            if (modelDayOfWeek in rule.daysOfWeek) {
                return candidateDate
            }
        }
    }

    private fun calculateNextMonthly(startDate: Instant, now: Instant, rule: RecurrenceRule): Instant {
        var next = startDate
        val timeZone = TimeZone.currentSystemDefault()
        while (next <= now) {
            next = next.plus(DateTimePeriod(months = rule.interval), timeZone)
        }
        return next
    }

    private fun calculateNextYearly(startDate: Instant, now: Instant, rule: RecurrenceRule): Instant {
        var next = startDate
        val timeZone = TimeZone.currentSystemDefault()
        while (next <= now) {
            next = next.plus(DateTimePeriod(years = rule.interval), timeZone)
        }
        return next
    }
}
