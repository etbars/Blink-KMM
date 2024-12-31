package com.example.alertapp.api

import kotlinx.coroutines.flow.Flow

interface ApiProvider {
    val config: ApiConfig

    suspend fun <R> get(
        endpoint: String,
        params: Map<String, String> = emptyMap()
    ): Flow<ApiResponse<R>>

    suspend fun <R> post(
        endpoint: String,
        body: Any? = null,
        params: Map<String, String> = emptyMap()
    ): Flow<ApiResponse<R>>

    fun validateConfig(): Boolean

    suspend fun <R> withRetry(
        maxAttempts: Int = 3,
        action: suspend () -> Flow<ApiResponse<R>>
    ): Flow<ApiResponse<R>>
}
