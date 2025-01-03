package com.example.alertapp.api.notification

import com.example.alertapp.api.BaseApiProvider
import com.example.alertapp.api.ApiResponse
import com.example.alertapp.api.errors.ApiError
import com.example.alertapp.models.Notification
import com.example.alertapp.network.NetworkClient
import io.ktor.client.call.*
import io.ktor.http.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import co.touchlab.kermit.Logger

class NotificationProvider(
    networkClient: NetworkClient,
    override val config: Map<String, String>,
    override val logger: Logger = Logger.withTag("NotificationProvider")
) : BaseApiProvider(networkClient) {
    override val baseUrl = config["NOTIFICATION_API_URL"] ?: throw IllegalStateException("NOTIFICATION_API_URL not configured")
    private val apiKey = config["NOTIFICATION_API_KEY"] ?: throw IllegalStateException("NOTIFICATION_API_KEY not configured")

    suspend fun getNotifications(
        userId: String,
        status: String? = null,
        page: Int = 1,
        pageSize: Int = 10
    ): Flow<ApiResponse<List<Notification>>> = flow {
        emit(ApiResponse.Loading)
        logger.d("Fetching notifications for user: $userId")
        emit(get(
            endpoint = "notifications",
            params = buildMap {
                put("userId", userId)
                put("page", page.toString())
                put("pageSize", pageSize.toString())
                status?.let { put("status", it) }
            }
        ))
    }

    suspend fun getNotification(id: String): Flow<ApiResponse<Notification>> = flow {
        emit(ApiResponse.Loading)
        logger.d("Fetching notification: $id")
        emit(get(
            endpoint = "notifications/$id"
        ))
    }

    suspend fun createNotification(notification: Notification): Flow<ApiResponse<Notification>> = flow {
        emit(ApiResponse.Loading)
        logger.d("Creating notification")
        emit(post(
            endpoint = "notifications",
            body = notification
        ))
    }

    suspend fun updateNotification(notification: Notification): Flow<ApiResponse<Notification>> = flow {
        emit(ApiResponse.Loading)
        logger.d("Updating notification: ${notification.id}")
        emit(post(
            endpoint = "notifications/${notification.id}",
            body = notification,
            params = mapOf("_method" to "PUT")
        ))
    }

    suspend fun deleteNotification(id: String): Flow<ApiResponse<Unit>> = flow {
        emit(ApiResponse.Loading)
        logger.d("Deleting notification: $id")
        emit(post(
            endpoint = "notifications/$id",
            body = mapOf<String, String>(),
            params = mapOf("_method" to "DELETE")
        ))
    }

    suspend fun markAsRead(id: String): Flow<ApiResponse<Unit>> = flow {
        emit(ApiResponse.Loading)
        logger.d("Marking notification as read: $id")
        emit(post(
            endpoint = "notifications/$id/read",
            body = mapOf<String, String>()
        ))
    }
}
