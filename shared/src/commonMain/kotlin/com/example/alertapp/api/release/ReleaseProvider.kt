package com.example.alertapp.api.release

import com.example.alertapp.api.ApiResponse
import com.example.alertapp.api.BaseApiProvider
import com.example.alertapp.api.errors.ApiError
import com.example.alertapp.models.Release
import com.example.alertapp.network.NetworkClient
import io.ktor.client.call.*
import io.ktor.http.*
import kotlinx.serialization.json.*
import co.touchlab.kermit.Logger
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

class ReleaseProvider(
    networkClient: NetworkClient,
    override val config: Map<String, String>,
    override val logger: Logger = Logger.withTag("ReleaseProvider")
) : BaseApiProvider(networkClient) {
    
    override val baseUrl = config["RELEASE_API_URL"] ?: throw IllegalStateException("RELEASE_API_URL not configured")

    suspend fun getReleases(
        platform: String? = null,
        category: String? = null,
        minRating: Double? = null,
        since: Instant? = null
    ): ApiResponse<List<Release>> = get<String>(
        endpoint = "releases",
        params = buildMap {
            platform?.let { put("platform", it) }
            category?.let { put("category", it) }
            minRating?.let { put("minRating", it.toString()) }
            since?.let { put("since", it.toString()) }
        }
    ).let { response ->
        when (response) {
            is ApiResponse.Success -> {
                try {
                    val jsonResponse = Json.parseToJsonElement(response.data).jsonObject
                    val releases = jsonResponse["releases"]?.jsonArray?.mapNotNull { releaseJson ->
                        try {
                            Release(
                                id = releaseJson.jsonObject["id"]?.jsonPrimitive?.content ?: return@mapNotNull null,
                                title = releaseJson.jsonObject["title"]?.jsonPrimitive?.content ?: "",
                                description = releaseJson.jsonObject["description"]?.jsonPrimitive?.content ?: "",
                                version = releaseJson.jsonObject["version"]?.jsonPrimitive?.content ?: "",
                                platform = releaseJson.jsonObject["platform"]?.jsonPrimitive?.content ?: "unknown",
                                category = releaseJson.jsonObject["category"]?.jsonPrimitive?.content,
                                url = releaseJson.jsonObject["url"]?.jsonPrimitive?.content ?: "",
                                rating = releaseJson.jsonObject["rating"]?.jsonPrimitive?.double,
                                publishedAt = releaseJson.jsonObject["publishedAt"]?.jsonPrimitive?.content?.let { 
                                    Instant.parse(it)
                                } ?: Clock.System.now(),
                                createdAt = releaseJson.jsonObject["createdAt"]?.jsonPrimitive?.content?.let { 
                                    Instant.parse(it)
                                } ?: Clock.System.now(),
                                updatedAt = releaseJson.jsonObject["updatedAt"]?.jsonPrimitive?.content?.let { 
                                    Instant.parse(it)
                                } ?: Clock.System.now()
                            )
                        } catch (e: Exception) {
                            logger.e("Failed to parse release", e)
                            null
                        }
                    } ?: emptyList()
                    ApiResponse.Success(releases)
                } catch (e: Exception) {
                    logger.e("Failed to parse releases response", e)
                    ApiResponse.Error(ApiError.ParseError("Failed to parse releases response: ${e.message}"))
                }
            }
            is ApiResponse.Error -> response
            is ApiResponse.Loading -> response
        }
    }

    suspend fun getRelease(id: String): ApiResponse<Release> = get<String>(
        endpoint = "releases/$id"
    ).let { response ->
        when (response) {
            is ApiResponse.Success -> {
                try {
                    val releaseJson = Json.parseToJsonElement(response.data).jsonObject
                    ApiResponse.Success(Release(
                        id = releaseJson["id"]?.jsonPrimitive?.content ?: throw Exception("Missing id"),
                        title = releaseJson["title"]?.jsonPrimitive?.content ?: "",
                        description = releaseJson["description"]?.jsonPrimitive?.content ?: "",
                        version = releaseJson["version"]?.jsonPrimitive?.content ?: "",
                        platform = releaseJson["platform"]?.jsonPrimitive?.content ?: "unknown",
                        category = releaseJson["category"]?.jsonPrimitive?.content,
                        url = releaseJson["url"]?.jsonPrimitive?.content ?: "",
                        rating = releaseJson["rating"]?.jsonPrimitive?.double,
                        publishedAt = releaseJson["publishedAt"]?.jsonPrimitive?.content?.let { 
                            Instant.parse(it)
                        } ?: Clock.System.now(),
                        createdAt = releaseJson["createdAt"]?.jsonPrimitive?.content?.let { 
                            Instant.parse(it)
                        } ?: Clock.System.now(),
                        updatedAt = releaseJson["updatedAt"]?.jsonPrimitive?.content?.let { 
                            Instant.parse(it)
                        } ?: Clock.System.now()
                    ))
                } catch (e: Exception) {
                    logger.e("Failed to parse release", e)
                    ApiResponse.Error(ApiError.ParseError("Failed to parse release: ${e.message}"))
                }
            }
            is ApiResponse.Error -> response
            is ApiResponse.Loading -> response
        }
    }
}
