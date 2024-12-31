package com.example.alertapp.android.api

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RateLimiter @Inject constructor() {
    private val timestamps = ConcurrentHashMap<String, MutableList<Long>>()
    private val mutex = Mutex()

    suspend fun checkRateLimit(key: String, maxRequests: Int, timeWindowMillis: Long): Boolean {
        return mutex.withLock {
            val now = System.currentTimeMillis()
            val requests = timestamps.getOrPut(key) { mutableListOf() }
            
            // Remove timestamps older than the time window
            requests.removeAll { timestamp -> 
                now - timestamp > timeWindowMillis 
            }

            // Check if we've exceeded the rate limit
            if (requests.size >= maxRequests) {
                val oldestTimestamp = requests.minOrNull() ?: return@withLock false
                val windowEnd = oldestTimestamp + timeWindowMillis
                val waitTime = windowEnd - now
                
                if (waitTime > 0) {
                    return@withLock false
                }
            }

            // Add current timestamp and return true
            requests.add(now)
            true
        }
    }

    suspend fun getRemainingWaitTime(key: String, maxRequests: Int, timeWindowMillis: Long): Long {
        return mutex.withLock {
            val now = System.currentTimeMillis()
            val requests = timestamps.getOrPut(key) { mutableListOf() }
            
            requests.removeAll { timestamp -> 
                now - timestamp > timeWindowMillis 
            }

            if (requests.size >= maxRequests) {
                val oldestTimestamp = requests.minOrNull() ?: return@withLock 0L
                val windowEnd = oldestTimestamp + timeWindowMillis
                val waitTime = windowEnd - now
                if (waitTime > 0) waitTime else 0L
            } else {
                0L
            }
        }
    }

    fun getFormattedWaitTime(waitTimeMillis: Long): String {
        val minutes = TimeUnit.MILLISECONDS.toMinutes(waitTimeMillis)
        val seconds = TimeUnit.MILLISECONDS.toSeconds(waitTimeMillis) % 60
        return when {
            minutes > 0 -> "$minutes minute${if (minutes > 1) "s" else ""} and $seconds second${if (seconds > 1) "s" else ""}"
            else -> "$seconds second${if (seconds > 1) "s" else ""}"
        }
    }
}
