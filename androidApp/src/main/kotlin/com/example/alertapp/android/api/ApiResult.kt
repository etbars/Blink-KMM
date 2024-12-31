package com.example.alertapp.android.api

sealed class ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>()
    data class Error(
        val code: ErrorCode,
        val message: String,
        val waitTime: Long = 0L
    ) : ApiResult<Nothing>()

    fun isSuccess() = this is Success
    fun isError() = this is Error

    fun getOrNull(): T? = when (this) {
        is Success -> data
        is Error -> null
    }

    fun getOrThrow(): T = when (this) {
        is Success -> data
        is Error -> throw ApiException(code, message)
    }
}

enum class ErrorCode {
    RATE_LIMIT_EXCEEDED,
    NETWORK_ERROR,
    API_ERROR,
    PARSE_ERROR,
    UNKNOWN_ERROR
}

class ApiException(
    val code: ErrorCode,
    override val message: String
) : Exception(message)
