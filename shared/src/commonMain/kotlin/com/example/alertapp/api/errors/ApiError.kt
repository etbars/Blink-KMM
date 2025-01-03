package com.example.alertapp.api.errors

import kotlinx.serialization.Serializable

@Serializable
sealed class ApiError : Throwable() {
    abstract override val message: String
    abstract val code: String?

    @Serializable
    data class UnknownError(
        override val message: String = "An unknown error occurred",
        override val code: String? = "UNKNOWN_ERROR"
    ) : ApiError()

    @Serializable
    data class NetworkError(
        override val message: String = "A network error occurred",
        override val code: String? = "NETWORK_ERROR"
    ) : ApiError()

    @Serializable
    data class ValidationError(
        override val message: String = "Invalid input provided",
        override val code: String? = "VALIDATION_ERROR",
        val field: String? = null
    ) : ApiError()

    @Serializable
    data class AuthenticationError(
        override val message: String = "Authentication failed",
        override val code: String? = "AUTH_ERROR"
    ) : ApiError()

    @Serializable
    data class NotFoundError(
        override val message: String = "Resource not found",
        override val code: String? = "NOT_FOUND"
    ) : ApiError()

    @Serializable
    data class ServerError(
        override val message: String = "Server error occurred",
        override val code: String? = "SERVER_ERROR"
    ) : ApiError()

    @Serializable
    data class ParseError(
        override val message: String = "Failed to parse response",
        override val code: String? = "PARSE_ERROR"
    ) : ApiError()

    @Serializable
    data class RateLimitError(
        override val message: String = "Rate limit exceeded",
        override val code: String? = "RATE_LIMIT"
    ) : ApiError()

    @Serializable
    data class NoDataError(
        override val message: String = "No data available",
        override val code: String? = "NO_DATA"
    ) : ApiError()

    @Serializable
    data class TimeoutError(
        override val message: String = "Request timed out",
        override val code: String? = "TIMEOUT"
    ) : ApiError()

    @Serializable
    data class RateLimitExceeded(
        override val message: String = "Rate limit exceeded",
        override val code: String? = "RATE_LIMIT_EXCEEDED",
        val retryAfter: Int? = null
    ) : ApiError()

    @Serializable
    data class HttpError(
        override val message: String,
        override val code: String?,
        val statusCode: Int
    ) : ApiError()

    companion object {
        fun fromException(e: Throwable): ApiError = when (e) {
            is ApiError -> e
            else -> UnknownError(e.message ?: "Unknown error occurred")
        }
    }
}
