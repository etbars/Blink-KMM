package com.example.alertapp.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Represents an API error response.
 * @property code The error code
 * @property message The error message
 * @property details Additional error details (optional)
 */
@Serializable
data class ApiError(
    @SerialName("code")
    val code: String,
    
    @SerialName("message")
    val message: String,
    
    @SerialName("details")
    val details: Map<String, String>? = null
) {
    companion object {
        // Common error codes
        const val INVALID_REQUEST = "INVALID_REQUEST"
        const val UNAUTHORIZED = "UNAUTHORIZED"
        const val FORBIDDEN = "FORBIDDEN"
        const val NOT_FOUND = "NOT_FOUND"
        const val INTERNAL_ERROR = "INTERNAL_ERROR"
        const val SERVICE_UNAVAILABLE = "SERVICE_UNAVAILABLE"
        const val NETWORK_ERROR = "NETWORK_ERROR"
        
        // Create helper functions for common errors
        fun unauthorized(message: String = "Unauthorized") = ApiError(UNAUTHORIZED, message)
        fun forbidden(message: String = "Forbidden") = ApiError(FORBIDDEN, message)
        fun notFound(message: String = "Not found") = ApiError(NOT_FOUND, message)
        fun internalError(message: String = "Internal server error") = ApiError(INTERNAL_ERROR, message)
        fun networkError(message: String = "Network error") = ApiError(NETWORK_ERROR, message)
    }
    
    /**
     * Checks if this error is a network-related error.
     */
    fun isNetworkError(): Boolean = code == NETWORK_ERROR
    
    /**
     * Checks if this error is an authentication error.
     */
    fun isAuthError(): Boolean = code == UNAUTHORIZED || code == FORBIDDEN
    
    /**
     * Checks if this error is a server error.
     */
    fun isServerError(): Boolean = code == INTERNAL_ERROR || code == SERVICE_UNAVAILABLE
    
    /**
     * Returns a user-friendly error message.
     */
    fun getUserMessage(): String = when (code) {
        NETWORK_ERROR -> "Please check your internet connection and try again."
        UNAUTHORIZED -> "Please log in to continue."
        FORBIDDEN -> "You don't have permission to perform this action."
        NOT_FOUND -> "The requested resource was not found."
        INTERNAL_ERROR -> "Something went wrong on our end. Please try again later."
        SERVICE_UNAVAILABLE -> "The service is temporarily unavailable. Please try again later."
        else -> message
    }
}
