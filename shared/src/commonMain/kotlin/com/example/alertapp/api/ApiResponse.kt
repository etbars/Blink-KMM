package com.example.alertapp.api

import com.example.alertapp.api.errors.ApiError

sealed class ApiResponse<out T> {
    object Loading : ApiResponse<Nothing>()
    
    data class Success<T>(
        val data: T,
        val metadata: Map<String, Any> = emptyMap()
    ) : ApiResponse<T>()
    
    data class Error(
        val error: ApiError,
        val data: Any? = null
    ) : ApiResponse<Nothing>()

    fun isSuccessful(): Boolean = this is Success

    fun getOrNull(): T? = when (this) {
        is Success -> data
        else -> null
    }

    fun getOrThrow(): T = when (this) {
        is Success -> data
        is Error -> throw RuntimeException(error.message)
        is Loading -> throw IllegalStateException("Response is still loading")
    }

    suspend fun onSuccess(block: suspend (T) -> Unit): ApiResponse<T> {
        if (this is Success) {
            block(data)
        }
        return this
    }

    suspend fun onError(block: suspend (ApiError) -> Unit): ApiResponse<T> {
        if (this is Error) {
            block(error)
        }
        return this
    }

    suspend fun onLoading(block: suspend () -> Unit): ApiResponse<T> {
        if (this is Loading) {
            block()
        }
        return this
    }

    companion object {
        fun <T> success(data: T, metadata: Map<String, Any> = emptyMap()) = Success(data, metadata)
        fun error(error: ApiError, data: Any? = null) = Error(error, data)
        fun <T> loading(): ApiResponse<T> = Loading
    }
}
