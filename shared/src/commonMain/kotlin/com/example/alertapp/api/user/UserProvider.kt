package com.example.alertapp.api.user

import com.example.alertapp.api.ApiResponse
import com.example.alertapp.api.BaseApiProvider
import com.example.alertapp.api.errors.ApiError
import com.example.alertapp.models.user.User
import com.example.alertapp.models.user.UserRole
import com.example.alertapp.network.NetworkClient
import io.ktor.client.call.*
import io.ktor.http.*
import kotlinx.serialization.json.*
import co.touchlab.kermit.Logger
import kotlinx.datetime.Clock

class UserProvider(
    networkClient: NetworkClient,
    override val config: Map<String, String>,
    override val logger: Logger = Logger.withTag("UserProvider")
) : BaseApiProvider(networkClient) {
    
    override val baseUrl = config["USER_API_URL"] ?: throw IllegalStateException("USER_API_URL not configured")
    private val apiKey = config["USER_API_KEY"] ?: throw IllegalStateException("USER_API_KEY not configured")

    suspend fun getUser(id: String): ApiResponse<User> = get<User>(
        endpoint = "users/$id",
        params = buildMap {
            put("apiKey", apiKey)
        }
    ).let { response ->
        when (response) {
            is ApiResponse.Success -> {
                try {
                    ApiResponse.Success(response.data)
                } catch (e: Exception) {
                    logger.e("Failed to parse user", e)
                    ApiResponse.Error(ApiError.ParseError("Failed to parse user: ${e.message}"))
                }
            }
            is ApiResponse.Error -> response
            is ApiResponse.Loading -> response
        }
    }

    suspend fun updateUser(id: String, user: User): ApiResponse<User> = put<User>(
        endpoint = "users/$id",
        body = user,
        params = buildMap {
            put("apiKey", apiKey)
        }
    ).let { response ->
        when (response) {
            is ApiResponse.Success -> {
                try {
                    ApiResponse.Success(response.data)
                } catch (e: Exception) {
                    logger.e("Failed to parse updated user", e)
                    ApiResponse.Error(ApiError.ParseError("Failed to parse updated user: ${e.message}"))
                }
            }
            is ApiResponse.Error -> response
            is ApiResponse.Loading -> response
        }
    }

    suspend fun deleteUser(id: String): ApiResponse<Unit> = delete<Unit>(
        endpoint = "users/$id",
        params = buildMap {
            put("apiKey", apiKey)
        }
    ).let { response ->
        when (response) {
            is ApiResponse.Success -> response
            is ApiResponse.Error -> response
            is ApiResponse.Loading -> response
        }
    }

    suspend fun searchUsers(
        query: String,
        role: UserRole? = null,
        status: String? = null,
        limit: Int = 20,
        offset: Int = 0
    ): ApiResponse<List<User>> = get<List<User>>(
        endpoint = "users/search",
        params = buildMap {
            put("query", query)
            role?.let { put("role", it.name.lowercase()) }
            status?.let { put("status", it) }
            put("limit", limit.toString())
            put("offset", offset.toString())
            put("apiKey", apiKey)
        }
    ).let { response ->
        when (response) {
            is ApiResponse.Success -> response
            is ApiResponse.Error -> response
            is ApiResponse.Loading -> response
        }
    }

    suspend fun createUser(
        username: String,
        email: String,
        role: UserRole = UserRole.USER,
        metadata: Map<String, String> = emptyMap()
    ): ApiResponse<User> = post<User>(
        endpoint = "users",
        body = buildMap {
            put("username", username)
            put("email", email)
            put("role", role.name.lowercase())
            put("metadata", metadata)
            put("created_at", Clock.System.now().toString())
        },
        params = buildMap {
            put("apiKey", apiKey)
        }
    ).let { response ->
        when (response) {
            is ApiResponse.Success -> response
            is ApiResponse.Error -> response
            is ApiResponse.Loading -> response
        }
    }
}
