package com.example.alertapp.api.event

import com.example.alertapp.api.ApiError
import com.example.alertapp.api.ApiResponse
import com.example.alertapp.api.BaseApiProvider
import com.example.alertapp.models.event.*
import kotlinx.serialization.json.*
import kotlinx.datetime.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import co.touchlab.kermit.Logger
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

class EventProvider(
    override val config: EventConfig
) : BaseApiProvider<EventConfig>() {
    private val logger = Logger.withTag("EventProvider")
    private val eventCache = EventCache()
    private val recurrenceCalculator = RecurrenceCalculator()

    suspend fun getEvents(
        timeRange: ClosedRange<Instant>,
        categories: Set<EventCategory>? = null
    ): Flow<ApiResponse<List<EventData.ScheduledEvent>>> = flow {
        try {
            logger.d { "Fetching events for range: ${timeRange.start} to ${timeRange.endInclusive}" }
            
            // First check cache
            val cachedEvents = eventCache.getEvents(timeRange)
            if (cachedEvents != null) {
                logger.d { "Returning ${cachedEvents.size} cached events" }
                emit(ApiResponse.Success(filterEvents(cachedEvents, categories)))
                return@flow
            }
            
            // Fetch from calendars
            val events = mutableListOf<EventData.ScheduledEvent>()
            config.calendars.forEach { calendarId ->
                try {
                    events.addAll(fetchCalendarEvents(calendarId, timeRange))
                } catch (e: Exception) {
                    logger.w(e) { "Failed to fetch events for calendar: $calendarId" }
                }
            }
            
            // Expand recurring events
            logger.d { "Expanding ${events.count { it.recurrence != null }} recurring events" }
            val expandedEvents = events.flatMap { event ->
                if (event.recurrence != null) {
                    recurrenceCalculator.expandRecurrence(event, timeRange)
                } else {
                    listOf(event)
                }
            }
            
            // Cache results
            eventCache.cacheEvents(expandedEvents)
            
            // Filter and emit
            val filteredEvents = filterEvents(expandedEvents, categories)
            logger.d { "Returning ${filteredEvents.size} filtered events" }
            emit(ApiResponse.Success(filteredEvents))
            
        } catch (e: Exception) {
            logger.e(e) { "Failed to get events" }
            emit(ApiResponse.Error(
                message = e.message ?: "Failed to get events",
                cause = e
            ))
        }
    }

    private suspend fun fetchCalendarEvents(
        calendarId: String,
        timeRange: ClosedRange<Instant>
    ): List<EventData.ScheduledEvent> {
        logger.d { "Fetching events for calendar: $calendarId" }
        
        val params = mapOf(
            "calendarId" to calendarId,
            "startTime" to timeRange.start.toString(),
            "endTime" to timeRange.endInclusive.toString()
        )
        
        var events = emptyList<EventData.ScheduledEvent>()
        get<JsonObject>("events", params).collect { response ->
            when (response) {
                is ApiResponse.Success -> {
                    events = parseCalendar(response.data).events
                }
                is ApiResponse.Loading -> {}
                is ApiResponse.Error -> {
                    logger.w { "Error fetching calendar $calendarId: ${response.message}" }
                }
            }
        }
        return events
    }

    private fun filterEvents(
        events: List<EventData.ScheduledEvent>,
        categories: Set<EventCategory>?
    ): List<EventData.ScheduledEvent> {
        return events.filter { event ->
            (categories == null || categories.contains(event.category)) &&
            matchesFilters(event)
        }
    }
    
    private fun matchesFilters(event: EventData.ScheduledEvent): Boolean {
        return with(config.filters) {
            (categories.isEmpty() || categories.contains(event.category.name)) &&
            (locations.isEmpty() || event.location?.let { loc -> 
                locations.contains(loc.name) 
            } ?: false) &&
            (!excludeDeclined || event.metadata["status"] != "declined") &&
            (!excludeCancelled || event.metadata["status"] != "cancelled")
        }
    }

    private fun parseCalendar(json: JsonObject): EventData.Calendar {
        try {
            val events = json["events"]?.jsonArray?.mapNotNull { eventJson ->
                try {
                    parseScheduledEvent(eventJson.jsonObject)
                } catch (e: Exception) {
                    logger.w(e) { "Failed to parse event" }
                    null
                }
            } ?: emptyList()
            
            return EventData.Calendar(
                id = json["id"]?.jsonPrimitive?.content 
                    ?: throw ApiError.ParseError("Missing calendar ID"),
                name = json["name"]?.jsonPrimitive?.content 
                    ?: throw ApiError.ParseError("Missing calendar name"),
                description = json["description"]?.jsonPrimitive?.contentOrNull,
                timeZone = TimeZone.of(
                    json["timeZone"]?.jsonPrimitive?.content 
                        ?: throw ApiError.ParseError("Missing timezone")
                ),
                events = events,
                metadata = json["metadata"]?.jsonObject?.let { parseMetadata(it) } ?: emptyMap()
            )
        } catch (e: Exception) {
            logger.e(e) { "Failed to parse calendar" }
            throw ApiError.ParseError("Failed to parse calendar: ${e.message}")
        }
    }
    
    private fun parseScheduledEvent(json: JsonObject): EventData.ScheduledEvent {
        return EventData.ScheduledEvent(
            id = json["id"]?.jsonPrimitive?.content 
                ?: throw ApiError.ParseError("Missing event ID"),
            title = json["title"]?.jsonPrimitive?.content 
                ?: throw ApiError.ParseError("Missing event title"),
            description = json["description"]?.jsonPrimitive?.contentOrNull,
            startTime = json["startTime"]?.jsonPrimitive?.content?.let { 
                try {
                    Instant.parse(it)
                } catch (e: Exception) {
                    throw ApiError.ParseError("Invalid start time format")
                }
            } ?: throw ApiError.ParseError("Missing start time"),
            endTime = json["endTime"]?.jsonPrimitive?.content?.let {
                try {
                    Instant.parse(it)
                } catch (e: Exception) {
                    throw ApiError.ParseError("Invalid end time format")
                }
            },
            location = json["location"]?.jsonObject?.let { parseLocation(it) },
            category = json["category"]?.jsonPrimitive?.content?.let {
                try {
                    EventCategory.valueOf(it)
                } catch (e: Exception) {
                    throw ApiError.ParseError("Invalid event category")
                }
            } ?: throw ApiError.ParseError("Missing event category"),
            recurrence = json["recurrence"]?.jsonObject?.let { parseRecurrenceRule(it) },
            reminders = json["reminders"]?.jsonArray?.mapNotNull { 
                try {
                    parseReminder(it.jsonObject)
                } catch (e: Exception) {
                    logger.w(e) { "Failed to parse reminder" }
                    null
                }
            } ?: emptyList(),
            metadata = json["metadata"]?.jsonObject?.let { parseMetadata(it) } ?: emptyMap()
        )
    }
    
    private fun parseLocation(json: JsonObject): Location {
        return Location(
            name = json["name"]?.jsonPrimitive?.contentOrNull,
            address = json["address"]?.jsonPrimitive?.contentOrNull,
            latitude = json["latitude"]?.jsonPrimitive?.doubleOrNull,
            longitude = json["longitude"]?.jsonPrimitive?.doubleOrNull,
            metadata = json["metadata"]?.jsonObject?.let { parseMetadata(it) } ?: emptyMap()
        )
    }
    
    private fun parseRecurrenceRule(json: JsonObject): RecurrenceRule {
        val daysOfWeek = json["daysOfWeek"]?.jsonArray?.mapNotNull { day ->
            try {
                DayOfWeek.valueOf(day.jsonPrimitive.content)
            } catch (e: Exception) {
                logger.w(e) { "Invalid day of week: ${day.jsonPrimitive.content}" }
                null
            }
        }?.toSet() ?: emptySet()
        
        return RecurrenceRule(
            frequency = json["frequency"]?.jsonPrimitive?.content?.let {
                try {
                    RecurrenceRule.Frequency.valueOf(it)
                } catch (e: Exception) {
                    throw ApiError.ParseError("Invalid recurrence frequency")
                }
            } ?: throw ApiError.ParseError("Missing recurrence frequency"),
            interval = json["interval"]?.jsonPrimitive?.int ?: 1,
            daysOfWeek = daysOfWeek,
            count = json["count"]?.jsonPrimitive?.int?.takeIf { it > 0 },
            endDate = json["endDate"]?.jsonPrimitive?.content?.let {
                try {
                    Instant.parse(it)
                } catch (e: Exception) {
                    throw ApiError.ParseError("Invalid end date format")
                }
            }
        )
    }
    
    private fun parseReminder(json: JsonObject): Reminder {
        return Reminder(
            beforeEvent = json["beforeEvent"]?.jsonPrimitive?.content?.let {
                try {
                    Duration.parse(it)
                } catch (e: Exception) {
                    throw ApiError.ParseError("Invalid duration format")
                }
            } ?: throw ApiError.ParseError("Missing reminder duration"),
            type = json["type"]?.jsonPrimitive?.content?.let {
                try {
                    Reminder.ReminderType.fromString(it)
                } catch (e: Exception) {
                    throw ApiError.ParseError("Invalid reminder type")
                }
            } ?: throw ApiError.ParseError("Missing reminder type"),
            metadata = json["metadata"]?.jsonObject?.let { parseMetadata(it) } ?: emptyMap()
        )
    }
    
    private fun parseMetadata(json: JsonObject): Map<String, String> {
        return buildMap {
            json.entries.forEach { (key, value) ->
                when (value) {
                    is JsonPrimitive -> put(key, value.content)
                    is JsonObject, is JsonArray -> put(key, value.toString())
                }
            }
        }
    }

    private class EventCache {
        private val cache = mutableMapOf<String, CacheEntry<List<EventData.ScheduledEvent>>>()

        fun getEvents(timeRange: ClosedRange<Instant>): List<EventData.ScheduledEvent>? {
            val cacheKey = buildCacheKey(timeRange)
            val entry = cache[cacheKey] ?: return null
            if (entry.isExpired()) {
                cache.remove(cacheKey)
                return null
            }
            return entry.data.filter { event ->
                event.startTime in timeRange || 
                (event.endTime?.let { it in timeRange } ?: false)
            }
        }

        fun cacheEvents(events: List<EventData.ScheduledEvent>) {
            val timeRange = events.fold(null as ClosedRange<Instant>?) { range, event ->
                val eventRange = event.startTime..(event.endTime ?: event.startTime)
                when {
                    range == null -> eventRange
                    else -> minOf(range.start, eventRange.start)..maxOf(range.endInclusive, eventRange.endInclusive)
                }
            }
            
            timeRange?.let { range ->
                cache[buildCacheKey(range)] = CacheEntry(events)
            }
        }

        private fun buildCacheKey(timeRange: ClosedRange<Instant>): String =
            "${timeRange.start}..${timeRange.endInclusive}"

        private data class CacheEntry<T>(
            val data: T,
            val timestamp: Instant = Clock.System.now()
        ) {
            fun isExpired(): Boolean =
                Clock.System.now() - timestamp > 5.minutes
        }
    }
}
