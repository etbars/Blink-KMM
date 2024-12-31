package com.example.alertapp.api

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.Json
import co.touchlab.kermit.Logger

abstract class BaseApiProvider<C : ApiConfig>(
    private val httpClient: HttpClient = HttpClient {
        install(ContentNegotiation) {
            json(Json {
                prettyPrint = true
                isLenient = true
                ignoreUnknownKeys = true
            })
        }
        install(HttpTimeout) {
            requestTimeoutMillis = config.timeout
            connectTimeoutMillis = config.timeout
            socketTimeoutMillis = config.timeout
        }
    }
) : ApiProvider {
    abstract override val config: C
    private val logger = Logger.withTag("BaseApiProvider")

    companion object {
        const val RETRY_DELAY_MS = 1000L
    }

    override suspend fun <R> get(
        endpoint: String,
        params: Map<String, String>
    ): Flow<ApiResponse<R>> = flow {
        emit(ApiResponse.Loading)
        try {
            val response = executeRequest<R> {
                httpClient.get(buildUrl(endpoint)) {
                    params.forEach { (key, value) ->
                        parameter(key, value)
                    }
                    setupRequest()
                }
            }
            emit(response)
        } catch (e: Exception) {
            emit(handleError<R>(e))
        }
    }

    override suspend fun <R> post(
        endpoint: String,
        body: Any?,
        params: Map<String, String>
    ): Flow<ApiResponse<R>> = flow {
        emit(ApiResponse.Loading)
        try {
            val response = executeRequest<R> {
                httpClient.post(buildUrl(endpoint)) {
                    params.forEach { (key, value) ->
                        parameter(key, value)
                    }
                    contentType(ContentType.Application.Json)
                    setBody(body)
                    setupRequest()
                }
            }
            emit(response)
        } catch (e: Exception) {
            emit(handleError<R>(e))
        }
    }

    override fun validateConfig(): Boolean {
        val apiKey = config.apiKey
        return config.baseUrl.isNotBlank() && 
               (apiKey == null || apiKey.isNotBlank())
    }

    override suspend fun <R> withRetry(
        maxAttempts: Int,
        action: suspend () -> Flow<ApiResponse<R>>
    ): Flow<ApiResponse<R>> = flow {
        var attempts = 0
        var lastError: ApiResponse.Error? = null

        while (attempts < maxAttempts) {
            action().collect { response ->
                when (response) {
                    is ApiResponse.Success -> {
                        emit(response)
                        return@flow
                    }
                    is ApiResponse.Loading -> emit(response)
                    is ApiResponse.Error -> {
                        lastError = response
                        when (response.cause) {
                            is ApiError.RateLimitError,
                            is ApiError.NetworkError -> {
                                if (attempts < maxAttempts - 1) {
                                    delay(RETRY_DELAY_MS * (attempts + 1))
                                    attempts++
                                } else {
                                    emit(response)
                                    return@flow
                                }
                            }
                            else -> {
                                emit(response)
                                return@flow
                            }
                        }
                    }
                }
            }
        }

        lastError?.let { emit(it) }
    }

    private fun buildUrl(endpoint: String): String {
        val baseUrl = config.baseUrl.trimEnd('/')
        val cleanEndpoint = endpoint.trimStart('/')
        return "$baseUrl/$cleanEndpoint"
    }

    private fun HttpRequestBuilder.setupRequest() {
        config.apiKey?.let {
            header("Authorization", "Bearer $it")
        }
    }

    private suspend inline fun <reified R> executeRequest(
        crossinline request: suspend () -> HttpResponse
    ): ApiResponse<R> {
        return try {
            val response = request()
            when (response.status) {
                HttpStatusCode.OK,
                HttpStatusCode.Created,
                HttpStatusCode.Accepted -> {
                    val body = response.body<R>()
                    ApiResponse.success(body)
                }
                HttpStatusCode.TooManyRequests -> {
                    val retryAfter = response.headers["Retry-After"]?.toLongOrNull()
                    throw ApiError.RateLimitError(retryAfter = retryAfter)
                }
                HttpStatusCode.Unauthorized -> {
                    throw ApiError.AuthenticationError()
                }
                else -> {
                    val errorBody = try {
                        response.body<ApiError.ErrorResponse>()
                    } catch (e: Exception) {
                        null
                    }
                    throw ApiError.HttpError(
                        message = errorBody?.error?.message ?: "HTTP ${response.status.value} error",
                        code = response.status.value,
                        details = errorBody?.error?.details?.mapValues { it.value as Any } ?: emptyMap()
                    )
                }
            }
        } catch (e: Exception) {
            handleError(e)
        }
    }

    private fun <R> handleError(error: Throwable): ApiResponse<R> {
        logger.e(error) { "API error occurred: ${error.message}" }
        
        val apiError = when (error) {
            is ApiError -> error
            is HttpRequestTimeoutException -> ApiError.NetworkError(
                message = "Request timed out",
                throwable = error
            )
            is io.ktor.client.network.sockets.ConnectTimeoutException -> ApiError.NetworkError(
                message = "Connection timed out",
                throwable = error
            )
            else -> ApiError.UnknownError(
                message = error.message ?: "Unknown error occurred",
                throwable = error
            )
        }
        
        return ApiResponse.error(apiError)
    }
}
