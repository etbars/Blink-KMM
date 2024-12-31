package com.example.alertapp.api.content

import com.example.alertapp.api.ApiError
import com.example.alertapp.api.ApiResponse
import com.example.alertapp.api.BaseApiProvider
import com.example.alertapp.models.content.*
import kotlinx.serialization.json.*
import kotlinx.datetime.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import co.touchlab.kermit.Logger

class ContentProvider(
    override val config: ContentConfig
) : BaseApiProvider<ContentConfig>() {
    private val logger = Logger.withTag("ContentProvider")
    private val cache = ContentCache()

    suspend fun fetchContent(
        source: ContentSource,
        limit: Int = config.batchSize
    ): Flow<ApiResponse<List<ContentData>>> = flow {
        try {
            logger.d { "Fetching content from source: $source" }
            
            // Check cache first
            val cacheKey = buildCacheKey(source)
            cache.getCachedContent(cacheKey)?.let { content ->
                logger.d { "Returning ${content.size} cached items" }
                emit(ApiResponse.Success(filterContent(content)))
                return@flow
            }
            
            // Make API request
            val params = mapOf(
                "source" to source.toString(),
                "limit" to limit.toString()
            )
            
            get<JsonObject>("content", params).collect { response ->
                when (response) {
                    is ApiResponse.Success -> {
                        val content = parseContentList(response.data)
                        cache.cacheContent(cacheKey, content)
                        emit(ApiResponse.Success(filterContent(content)))
                    }
                    is ApiResponse.Loading -> emit(ApiResponse.Loading)
                    is ApiResponse.Error -> emit(response)
                }
            }
        } catch (e: Exception) {
            logger.e(e) { "Failed to fetch content" }
            emit(ApiResponse.Error(
                message = "Failed to fetch content: ${e.message}",
                cause = e
            ))
        }
    }

    private fun parseContentList(json: JsonObject): List<ContentData> {
        return json["items"]?.jsonArray?.mapNotNull { item ->
            try {
                when (item.jsonObject["type"]?.jsonPrimitive?.content) {
                    "article" -> parseArticle(item.jsonObject)
                    "social_post" -> parseSocialPost(item.jsonObject)
                    else -> {
                        logger.w { "Unknown content type: ${item.jsonObject["type"]}" }
                        null
                    }
                }
            } catch (e: Exception) {
                logger.w(e) { "Failed to parse content item" }
                null
            }
        } ?: emptyList()
    }

    private fun parseArticle(json: JsonObject): ContentData.Article {
        return ContentData.Article(
            id = json["id"]?.jsonPrimitive?.content 
                ?: throw ApiError.ParseError("Missing article ID"),
            title = json["title"]?.jsonPrimitive?.content 
                ?: throw ApiError.ParseError("Missing article title"),
            content = json["content"]?.jsonPrimitive?.content 
                ?: throw ApiError.ParseError("Missing article content"),
            source = json["source"]?.jsonPrimitive?.content?.let {
                try {
                    ContentSource.valueOf(it)
                } catch (e: Exception) {
                    throw ApiError.ParseError("Invalid content source")
                }
            } ?: throw ApiError.ParseError("Missing content source"),
            author = json["author"]?.jsonPrimitive?.contentOrNull,
            publishedAt = json["publishedAt"]?.jsonPrimitive?.content?.let {
                try {
                    Instant.parse(it)
                } catch (e: Exception) {
                    throw ApiError.ParseError("Invalid published date format")
                }
            } ?: throw ApiError.ParseError("Missing published date"),
            url = json["url"]?.jsonPrimitive?.content 
                ?: throw ApiError.ParseError("Missing article URL"),
            keywords = json["keywords"]?.jsonArray?.mapNotNull { 
                it.jsonPrimitive.contentOrNull 
            }?.toSet() ?: emptySet(),
            sentiment = json["sentiment"]?.jsonPrimitive?.contentOrNull?.let {
                try {
                    Sentiment.valueOf(it)
                } catch (e: Exception) {
                    logger.w(e) { "Invalid sentiment value: $it" }
                    null
                }
            },
            relevanceScore = json["relevanceScore"]?.jsonPrimitive?.double 
                ?: throw ApiError.ParseError("Missing relevance score")
        )
    }

    private fun parseSocialPost(json: JsonObject): ContentData.SocialPost {
        val engagement = json["engagement"]?.jsonObject 
            ?: throw ApiError.ParseError("Missing engagement data")
            
        return ContentData.SocialPost(
            id = json["id"]?.jsonPrimitive?.content 
                ?: throw ApiError.ParseError("Missing post ID"),
            platform = json["platform"]?.jsonPrimitive?.content?.let {
                try {
                    Platform.valueOf(it)
                } catch (e: Exception) {
                    throw ApiError.ParseError("Invalid platform")
                }
            } ?: throw ApiError.ParseError("Missing platform"),
            source = ContentSource.SOCIAL,
            content = json["content"]?.jsonPrimitive?.content 
                ?: throw ApiError.ParseError("Missing post content"),
            author = json["author"]?.jsonPrimitive?.content 
                ?: throw ApiError.ParseError("Missing post author"),
            postedAt = json["postedAt"]?.jsonPrimitive?.content?.let {
                try {
                    Instant.parse(it)
                } catch (e: Exception) {
                    throw ApiError.ParseError("Invalid posted date format")
                }
            } ?: throw ApiError.ParseError("Missing posted date"),
            engagement = Engagement(
                likes = engagement["likes"]?.jsonPrimitive?.int ?: 0,
                shares = engagement["shares"]?.jsonPrimitive?.int ?: 0,
                comments = engagement["comments"]?.jsonPrimitive?.int ?: 0
            ),
            keywords = json["keywords"]?.jsonArray?.mapNotNull { 
                it.jsonPrimitive.contentOrNull 
            }?.toSet() ?: emptySet(),
            sentiment = json["sentiment"]?.jsonPrimitive?.contentOrNull?.let {
                try {
                    Sentiment.valueOf(it)
                } catch (e: Exception) {
                    logger.w(e) { "Invalid sentiment value: $it" }
                    null
                }
            }
        )
    }

    private fun filterContent(content: List<ContentData>): List<ContentData> {
        return content.filter { item ->
            with(config.filters) {
                when (item) {
                    is ContentData.Article -> {
                        item.relevanceScore >= minRelevanceScore &&
                        !excludedSources.contains(item.source.name) &&
                        !excludedAuthors.contains(item.author) &&
                        (sentiments.isEmpty() || item.sentiment?.let { 
                            sentiments.contains(it.name) 
                        } ?: false) &&
                        (keywords.isEmpty() || item.keywords.any { 
                            keywords.contains(it) 
                        })
                    }
                    is ContentData.SocialPost -> {
                        item.engagement.total() >= minEngagement &&
                        !excludedAuthors.contains(item.author) &&
                        (sentiments.isEmpty() || item.sentiment?.let { 
                            sentiments.contains(it.name) 
                        } ?: false) &&
                        (keywords.isEmpty() || item.keywords.any { 
                            keywords.contains(it) 
                        })
                    }
                }
            }
        }
    }

    private fun buildCacheKey(source: ContentSource): String = source.name

    private class ContentCache {
        private val cache = mutableMapOf<String, CacheEntry<List<ContentData>>>()

        fun getCachedContent(key: String): List<ContentData>? {
            val entry = cache[key] ?: return null
            if (entry.isExpired()) {
                cache.remove(key)
                return null
            }
            return entry.data
        }

        fun cacheContent(key: String, content: List<ContentData>) {
            cache[key] = CacheEntry(content)
        }

        private data class CacheEntry<T>(
            val data: T,
            val timestamp: Instant = Clock.System.now()
        ) {
            fun isExpired(): Boolean =
                Clock.System.now() - timestamp > 5.minutes
        }
    }
}
