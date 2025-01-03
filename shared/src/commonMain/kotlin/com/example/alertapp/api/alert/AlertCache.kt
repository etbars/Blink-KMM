package com.example.alertapp.api.alert

import com.example.alertapp.models.Alert
import kotlinx.datetime.Clock
import kotlin.time.Duration.Companion.minutes

class AlertCache {
    private val cache = mutableMapOf<String, CacheEntry>()
    private val cacheTimeout = 15.minutes

    fun cacheAlert(alert: Alert) {
        cache[alert.id] = CacheEntry(alert, Clock.System.now())
    }

    fun getAlert(id: String): Alert? {
        val entry = cache[id] ?: return null
        val now = Clock.System.now()
        
        return if (now - entry.timestamp > cacheTimeout) {
            cache.remove(id)
            null
        } else {
            entry.alert
        }
    }

    fun removeAlert(id: String) {
        cache.remove(id)
    }

    fun clear() {
        cache.clear()
    }

    private data class CacheEntry(
        val alert: Alert,
        val timestamp: kotlinx.datetime.Instant
    )
}
