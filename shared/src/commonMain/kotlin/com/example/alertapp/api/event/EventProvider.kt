package com.example.alertapp.api.event

import com.example.alertapp.api.ApiResponse
import com.example.alertapp.api.BaseApiProvider
import com.example.alertapp.api.errors.ApiError
import com.example.alertapp.models.Event
import com.example.alertapp.network.NetworkClient
import io.ktor.client.call.*
import io.ktor.http.*
import co.touchlab.kermit.Logger

class EventProvider(
    networkClient: NetworkClient,
    override val config: Map<String, String>,
    override val logger: Logger = Logger.withTag("EventProvider")
) : BaseApiProvider(networkClient) {

    override val baseUrl = config["EVENT_API_URL"] ?: throw IllegalStateException("EVENT_API_URL not configured")

    suspend fun getEvent(id: String): ApiResponse<Event> = get(
        endpoint = "events/$id"
    )

    suspend fun createEvent(event: Event): ApiResponse<Event> = post(
        endpoint = "events",
        body = event
    )

    suspend fun updateEvent(id: String, event: Event): ApiResponse<Event> = post(
        endpoint = "events/$id",
        body = event,
        params = mapOf("_method" to "PUT") // Some APIs require this for PUT requests
    )

    suspend fun deleteEvent(id: String): ApiResponse<Unit> = post(
        endpoint = "events/$id",
        body = mapOf<String, String>(), // Empty body
        params = mapOf("_method" to "DELETE") // Some APIs require this for DELETE requests
    )

    suspend fun listEvents(
        page: Int = 1,
        pageSize: Int = 20,
        filter: Map<String, String> = emptyMap()
    ): ApiResponse<List<Event>> = get(
        endpoint = "events",
        params = buildMap {
            put("page", page.toString())
            put("pageSize", pageSize.toString())
            putAll(filter)
        }
    )
}
