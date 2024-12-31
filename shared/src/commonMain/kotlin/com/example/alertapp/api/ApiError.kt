package com.example.alertapp.api

import kotlinx.serialization.Serializable

sealed class ApiError(
    open val message: String,
    open val code: Int? = null,
    open val details: Map<String, Any> = emptyMap()
) {
    class NetworkError(
        override val message: String = "Network error occurred",
        override val details: Map<String, Any> = emptyMap(),
        val throwable: Throwable? = null
    ) : ApiError(message)

    class HttpError(
        override val message: String,
        override val code: Int,
        override val details: Map<String, Any> = emptyMap()
    ) : ApiError(message, code)

    class RateLimitError(
        override val message: String = "Rate limit exceeded",
        val retryAfter: Long? = null
    ) : ApiError(message)

    class ParseError(
        override val message: String = "Failed to parse response",
        val rawResponse: String? = null,
        val throwable: Throwable? = null
    ) : ApiError(message)

    class ValidationError(
        override val message: String = "Validation failed",
        override val details: Map<String, Any> = emptyMap()
    ) : ApiError(message)

    class AuthenticationError(
        override val message: String = "Authentication failed",
        override val code: Int? = 401
    ) : ApiError(message, code)

    class UnknownError(
        override val message: String = "An unknown error occurred",
        val throwable: Throwable? = null
    ) : ApiError(message)

    @Serializable
    data class ErrorResponse(
        val error: ErrorDetails
    )

    @Serializable
    data class ErrorDetails(
        val message: String,
        val code: Int? = null,
        val details: Map<String, String> = emptyMap()
    )
}
