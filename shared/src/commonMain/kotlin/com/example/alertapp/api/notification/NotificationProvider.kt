package com.example.alertapp.api.notification

import com.example.alertapp.api.ApiError
import com.example.alertapp.api.ApiResponse
import com.example.alertapp.api.BaseApiProvider
import com.example.alertapp.models.Notification
import com.example.alertapp.models.NotificationPriority
import kotlinx.serialization.json.*
import kotlinx.datetime.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import co.touchlab.kermit.Logger

class NotificationProvider(
    override val config: NotificationConfig
) : BaseApiProvider<NotificationConfig>() {
    private val logger = Logger.withTag("NotificationProvider")
    private val cache = NotificationCache()

    suspend fun sendNotification(notification: Notification): Flow<ApiResponse<Notification>> = flow {
        try {
            logger.d { "Sending notification: ${notification.title}" }
            
            post<JsonObject>("notifications", emptyMap(), notification).collect { response ->
                when (response) {
                    is ApiResponse.Success -> {
                        val sentNotification = parseNotification(response.data)
                        cache.cacheNotification(sentNotification)
                        emit(ApiResponse.Success(sentNotification))
                    }
                    is ApiResponse.Loading -> emit(ApiResponse.Loading)
                    is ApiResponse.Error -> emit(response)
                }
            }
        } catch (e: Exception) {
            logger.e(e) { "Failed to send notification" }
            emit(ApiResponse.Error(
                message = "Failed to send notification: ${e.message}",
                cause = e
            ))
        }
    }

    suspend fun getNotification(notificationId: String): Flow<ApiResponse<Notification>> = flow {
        try {
            logger.d { "Fetching notification: $notificationId" }
            
            // Check cache first
            cache.getNotification(notificationId)?.let { notification ->
                logger.d { "Returning cached notification" }
                emit(ApiResponse.Success(notification))
                return@flow
            }
            
            get<JsonObject>("notifications/$notificationId", emptyMap()).collect { response ->
                when (response) {
                    is ApiResponse.Success -> {
                        val notification = parseNotification(response.data)
                        cache.cacheNotification(notification)
                        emit(ApiResponse.Success(notification))
                    }
                    is ApiResponse.Loading -> emit(ApiResponse.Loading)
                    is ApiResponse.Error -> emit(response)
                }
            }
        } catch (e: Exception) {
            logger.e(e) { "Failed to get notification" }
            emit(ApiResponse.Error(
                message = "Failed to get notification: ${e.message}",
                cause = e
            ))
        }
    }

    suspend fun getNotifications(
        startTime: Long? = null,
        endTime: Long? = null,
        priority: NotificationPriority? = null,
        limit: Int = config.maxBatchSize
    ): Flow<ApiResponse<List<Notification>>> = flow {
        try {
            logger.d { "Fetching notifications" }
            
            // Build cache key
            val cacheKey = buildCacheKey(startTime, endTime, priority)
            
            // Check cache first
            cache.getNotifications(cacheKey)?.let { notifications ->
                logger.d { "Returning ${notifications.size} cached notifications" }
                emit(ApiResponse.Success(filterNotifications(notifications)))
                return@flow
            }
            
            // Build parameters
            val params = buildMap {
                put("limit", limit.toString())
                startTime?.let { put("startTime", it.toString()) }
                endTime?.let { put("endTime", it.toString()) }
                priority?.let { put("priority", it.name) }
            }
            
            get<JsonObject>("notifications", params).collect { response ->
                when (response) {
                    is ApiResponse.Success -> {
                        val notifications = parseNotificationList(response.data)
                        cache.cacheNotifications(cacheKey, notifications)
                        emit(ApiResponse.Success(filterNotifications(notifications)))
                    }
                    is ApiResponse.Loading -> emit(ApiResponse.Loading)
                    is ApiResponse.Error -> emit(response)
                }
            }
        } catch (e: Exception) {
            logger.e(e) { "Failed to get notifications" }
            emit(ApiResponse.Error(
                message = "Failed to get notifications: ${e.message}",
                cause = e
            ))
        }
    }

    suspend fun deleteNotification(notificationId: String): Flow<ApiResponse<Boolean>> = flow {
        try {
            logger.d { "Deleting notification: $notificationId" }
            
            delete<JsonObject>("notifications/$notificationId", emptyMap()).collect { response ->
                when (response) {
                    is ApiResponse.Success -> {
                        cache.removeNotification(notificationId)
                        emit(ApiResponse.Success(true))
                    }
                    is ApiResponse.Loading -> emit(ApiResponse.Loading)
                    is ApiResponse.Error -> emit(response)
                }
            }
        } catch (e: Exception) {
            logger.e(e) { "Failed to delete notification" }
            emit(ApiResponse.Error(
                message = "Failed to delete notification: ${e.message}",
                cause = e
            ))
        }
    }

    private fun parseNotificationList(json: JsonObject): List<Notification> {
        return json["notifications"]?.jsonArray?.mapNotNull { notificationJson ->
            try {
                parseNotification(notificationJson.jsonObject)
            } catch (e: Exception) {
                logger.w(e) { "Failed to parse notification" }
                null
            }
        } ?: emptyList()
    }

    private fun parseNotification(json: JsonObject): Notification {
        return Notification(
            id = json["id"]?.jsonPrimitive?.content 
                ?: throw ApiError.ParseError("Missing notification ID"),
            title = json["title"]?.jsonPrimitive?.content 
                ?: throw ApiError.ParseError("Missing notification title"),
            message = json["message"]?.jsonPrimitive?.content 
                ?: throw ApiError.ParseError("Missing notification message"),
            timestamp = json["timestamp"]?.jsonPrimitive?.long 
                ?: Clock.System.now().toEpochMilliseconds(),
            priority = json["priority"]?.jsonPrimitive?.content?.let {
                try {
                    NotificationPriority.valueOf(it)
                } catch (e: Exception) {
                    logger.w(e) { "Invalid priority value: $it" }
                    config.defaultPriority
                }
            } ?: config.defaultPriority
        )
    }

    private fun filterNotifications(notifications: List<Notification>): List<Notification> {
        return notifications.filter { notification ->
            with(config.filters) {
                (priorities.isEmpty() || priorities.contains(notification.priority)) &&
                (minTimestamp == null || notification.timestamp >= minTimestamp) &&
                (maxTimestamp == null || notification.timestamp <= maxTimestamp) &&
                (searchQuery == null || matchesSearchQuery(notification, searchQuery))
            }
        }
    }

    private fun matchesSearchQuery(notification: Notification, query: String): Boolean {
        val lowercaseQuery = query.lowercase()
        return notification.title.lowercase().contains(lowercaseQuery) ||
               notification.message.lowercase().contains(lowercaseQuery)
    }

    private fun buildCacheKey(
        startTime: Long?,
        endTime: Long?,
        priority: NotificationPriority?
    ): String = buildString {
        append("notifications")
        startTime?.let { append(":start=$it") }
        endTime?.let { append(":end=$it") }
        priority?.let { append(":priority=${it.name}") }
    }

    private class NotificationCache {
        private val notificationCache = mutableMapOf<String, CacheEntry<Notification>>()
        private val listCache = mutableMapOf<String, CacheEntry<List<Notification>>>()

        fun getNotification(notificationId: String): Notification? {
            val entry = notificationCache[notificationId] ?: return null
            if (entry.isExpired()) {
                notificationCache.remove(notificationId)
                return null
            }
            return entry.data
        }

        fun getNotifications(cacheKey: String): List<Notification>? {
            val entry = listCache[cacheKey] ?: return null
            if (entry.isExpired()) {
                listCache.remove(cacheKey)
                return null
            }
            return entry.data
        }

        fun cacheNotification(notification: Notification) {
            notificationCache[notification.id] = CacheEntry(notification)
        }

        fun cacheNotifications(cacheKey: String, notifications: List<Notification>) {
            listCache[cacheKey] = CacheEntry(notifications)
            notifications.forEach { notification ->
                notificationCache[notification.id] = CacheEntry(notification)
            }
        }

        fun removeNotification(notificationId: String) {
            notificationCache.remove(notificationId)
            listCache.values.forEach { entry ->
                entry.data.find { it.id == notificationId }?.let {
                    // Remove the notification from all list caches that contain it
                    listCache.entries.removeIf { (_, cacheEntry) ->
                        cacheEntry.data.any { notification -> notification.id == notificationId }
                    }
                }
            }
        }

        private data class CacheEntry<T>(
            val data: T,
            val timestamp: Instant = Clock.System.now()
        ) {
            fun isExpired(): Boolean =
                Clock.System.now() - timestamp > 5.minutes
        }
    }
}
