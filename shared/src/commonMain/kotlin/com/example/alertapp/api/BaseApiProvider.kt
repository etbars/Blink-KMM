package com.example.alertapp.api

import com.example.alertapp.api.errors.ApiError
import com.example.alertapp.api.ApiResponse
import com.example.alertapp.network.NetworkClient
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import co.touchlab.kermit.Logger

abstract class BaseApiProvider(
    protected val networkClient: NetworkClient
) {
    protected abstract val baseUrl: String
    protected abstract val config: Map<String, String>
    protected abstract val logger: Logger

    protected suspend inline fun <reified T> get(
        endpoint: String,
        params: Map<String, String> = emptyMap()
    ): ApiResponse<T> {
        return try {
            val response = networkClient.client.get("$baseUrl/$endpoint") {
                params.forEach { (key, value) ->
                    parameter(key, value)
                }
                config.forEach { (key, value) ->
                    parameter(key, value)
                }
            }
            when (response.status) {
                HttpStatusCode.OK -> ApiResponse.Success(response.body())
                HttpStatusCode.NotFound -> ApiResponse.Error(ApiError.NotFoundError("Resource not found at $endpoint"))
                HttpStatusCode.Unauthorized -> ApiResponse.Error(ApiError.AuthenticationError("Authentication failed"))
                HttpStatusCode.BadRequest -> ApiResponse.Error(ApiError.ValidationError("Invalid request parameters"))
                else -> {
                    logger.e("HTTP error ${response.status}")
                    ApiResponse.Error(ApiError.ServerError("Server error: ${response.status}"))
                }
            }
        } catch (e: Exception) {
            logger.e("API Error", e)
            ApiResponse.Error(ApiError.NetworkError(e.message ?: "Network error occurred"))
        }
    }

    protected suspend inline fun <reified T> post(
        endpoint: String,
        body: Any,
        params: Map<String, String> = emptyMap()
    ): ApiResponse<T> {
        return try {
            val response = networkClient.client.post("$baseUrl/$endpoint") {
                contentType(ContentType.Application.Json)
                setBody(body)
                params.forEach { (key, value) ->
                    parameter(key, value)
                }
                config.forEach { (key, value) ->
                    parameter(key, value)
                }
            }
            when (response.status) {
                HttpStatusCode.OK, HttpStatusCode.Created -> ApiResponse.Success(response.body())
                HttpStatusCode.NotFound -> ApiResponse.Error(ApiError.NotFoundError("Resource not found at $endpoint"))
                HttpStatusCode.Unauthorized -> ApiResponse.Error(ApiError.AuthenticationError("Authentication failed"))
                HttpStatusCode.BadRequest -> ApiResponse.Error(ApiError.ValidationError("Invalid request parameters"))
                else -> {
                    logger.e("HTTP error ${response.status}")
                    ApiResponse.Error(ApiError.ServerError("Server error: ${response.status}"))
                }
            }
        } catch (e: Exception) {
            logger.e("API Error", e)
            ApiResponse.Error(ApiError.NetworkError(e.message ?: "Network error occurred"))
        }
    }

    protected suspend inline fun <reified T> put(
        endpoint: String,
        body: Any,
        params: Map<String, String> = emptyMap()
    ): ApiResponse<T> {
        return try {
            val response = networkClient.client.put("$baseUrl/$endpoint") {
                contentType(ContentType.Application.Json)
                setBody(body)
                params.forEach { (key, value) ->
                    parameter(key, value)
                }
                config.forEach { (key, value) ->
                    parameter(key, value)
                }
            }
            when (response.status) {
                HttpStatusCode.OK -> ApiResponse.Success(response.body())
                HttpStatusCode.NotFound -> ApiResponse.Error(ApiError.NotFoundError("Resource not found at $endpoint"))
                HttpStatusCode.Unauthorized -> ApiResponse.Error(ApiError.AuthenticationError("Authentication failed"))
                HttpStatusCode.BadRequest -> ApiResponse.Error(ApiError.ValidationError("Invalid request parameters"))
                else -> {
                    logger.e("HTTP error ${response.status}")
                    ApiResponse.Error(ApiError.ServerError("Server error: ${response.status}"))
                }
            }
        } catch (e: Exception) {
            logger.e("API Error", e)
            ApiResponse.Error(ApiError.NetworkError(e.message ?: "Network error occurred"))
        }
    }

    protected suspend inline fun <reified T> delete(
        endpoint: String,
        params: Map<String, String> = emptyMap()
    ): ApiResponse<T> {
        return try {
            val response = networkClient.client.delete("$baseUrl/$endpoint") {
                params.forEach { (key, value) ->
                    parameter(key, value)
                }
                config.forEach { (key, value) ->
                    parameter(key, value)
                }
            }
            when (response.status) {
                HttpStatusCode.OK, HttpStatusCode.NoContent -> ApiResponse.Success(response.body())
                HttpStatusCode.NotFound -> ApiResponse.Error(ApiError.NotFoundError("Resource not found at $endpoint"))
                HttpStatusCode.Unauthorized -> ApiResponse.Error(ApiError.AuthenticationError("Authentication failed"))
                HttpStatusCode.BadRequest -> ApiResponse.Error(ApiError.ValidationError("Invalid request parameters"))
                else -> {
                    logger.e("HTTP error ${response.status}")
                    ApiResponse.Error(ApiError.ServerError("Server error: ${response.status}"))
                }
            }
        } catch (e: Exception) {
            logger.e("API Error", e)
            ApiResponse.Error(ApiError.NetworkError(e.message ?: "Network error occurred"))
        }
    }
}
