package com.example.alertapp.api.content

import com.example.alertapp.api.ApiResponse
import com.example.alertapp.api.BaseApiProvider
import com.example.alertapp.api.errors.ApiError
import com.example.alertapp.models.content.Content
import com.example.alertapp.models.content.ContentFilter
import com.example.alertapp.config.AppConfig
import com.example.alertapp.network.NetworkClient
import io.ktor.client.call.*
import io.ktor.http.*
import co.touchlab.kermit.Logger

class ContentProvider(
    networkClient: NetworkClient,
    override val config: Map<String, String>,
    private val appConfig: AppConfig
) : BaseApiProvider(networkClient) {
    
    override val baseUrl: String = config["CONTENT_API_URL"] ?: throw IllegalStateException("CONTENT_API_URL not configured")
    override val logger: Logger = Logger.withTag("ContentProvider")

    suspend fun getContent(
        filter: ContentFilter,
        page: Int = 1,
        pageSize: Int = 20
    ): ApiResponse<List<Content>> = get<List<Content>>(
        endpoint = "content",
        params = buildMap {
            put("page", page.toString())
            put("pageSize", pageSize.toString())
            filter.query?.let { put("query", it) }
            filter.category?.let { put("category", it) }
            filter.source?.let { put("source", it) }
            filter.fromDate?.let { put("fromDate", it.toString()) }
            filter.toDate?.let { put("toDate", it.toString()) }
            if (filter.keywords.isNotEmpty()) {
                put("keywords", filter.keywords.joinToString(","))
                put("mustIncludeAllKeywords", filter.mustIncludeAllKeywords.toString())
            }
            if (filter.excludeKeywords.isNotEmpty()) {
                put("excludeKeywords", filter.excludeKeywords.joinToString(","))
            }
            filter.minEngagement?.let { put("minEngagement", it.toString()) }
            filter.maxAge?.let { put("maxAge", it.toString()) }
            if (filter.languages.isNotEmpty()) {
                put("languages", filter.languages.joinToString(","))
            }
        }
    ).let { response ->
        when (response) {
            is ApiResponse.Success -> response
            is ApiResponse.Error -> response
            is ApiResponse.Loading -> response
        }
    }

    suspend fun searchContent(
        query: String,
        filter: ContentFilter,
        page: Int = 1,
        pageSize: Int = 20
    ): ApiResponse<List<Content>> = get<List<Content>>(
        endpoint = "content/search",
        params = buildMap {
            put("query", query)
            put("page", page.toString())
            put("pageSize", pageSize.toString())
            filter.category?.let { put("category", it) }
            filter.source?.let { put("source", it) }
            filter.fromDate?.let { put("fromDate", it.toString()) }
            filter.toDate?.let { put("toDate", it.toString()) }
            if (filter.keywords.isNotEmpty()) {
                put("keywords", filter.keywords.joinToString(","))
                put("mustIncludeAllKeywords", filter.mustIncludeAllKeywords.toString())
            }
            if (filter.excludeKeywords.isNotEmpty()) {
                put("excludeKeywords", filter.excludeKeywords.joinToString(","))
            }
            filter.minEngagement?.let { put("minEngagement", it.toString()) }
            filter.maxAge?.let { put("maxAge", it.toString()) }
            if (filter.languages.isNotEmpty()) {
                put("languages", filter.languages.joinToString(","))
            }
        }
    ).let { response ->
        when (response) {
            is ApiResponse.Success -> response
            is ApiResponse.Error -> response
            is ApiResponse.Loading -> response
        }
    }

    suspend fun getContentById(id: String): ApiResponse<Content> = get<Content>(
        endpoint = "content/$id"
    ).let { response ->
        when (response) {
            is ApiResponse.Success -> response
            is ApiResponse.Error -> response
            is ApiResponse.Loading -> response
        }
    }
}
