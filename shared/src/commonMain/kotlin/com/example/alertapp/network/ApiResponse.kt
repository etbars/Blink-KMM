package com.example.alertapp.network

import kotlinx.serialization.Serializable

sealed class ApiResponse<out T> {
    data class Success<T>(val data: T) : ApiResponse<T>()
    
    @Serializable
    data class Error(
        val message: String,
        val details: Map<String, String> = emptyMap()
    ) : ApiResponse<Nothing>()
}

class NetworkException(
    val code: Int,
    override val message: String,
    val details: Map<String, String> = emptyMap()
) : Exception(message)
