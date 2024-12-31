package com.example.alertapp.api.news

import com.example.alertapp.api.ApiError
import com.example.alertapp.api.ApiResponse
import com.example.alertapp.api.BaseApiProvider
import com.example.alertapp.models.news.NewsArticle
import com.example.alertapp.models.news.NewsSearchQuery
import com.example.alertapp.models.news.NewsSource
import kotlinx.serialization.json.*
import co.touchlab.kermit.Logger
import kotlinx.datetime.Instant
import kotlin.time.Duration.Companion.minutes

class NewsApiProvider(
    override val config: NewsConfig
) : BaseApiProvider<NewsConfig>() {
    private val logger = Logger.withTag("NewsApiProvider")
    private val cache = NewsCache()

    suspend fun getTopHeadlines(
        country: String? = null,
        category: String? = null,
        sources: String? = null,
        query: String? = null,
        pageSize: Int? = null,
        page: Int? = null
    ): ApiResponse<List<NewsArticle>> {
        return try {
            logger.d { "Fetching top headlines" }
            
            // Check cache first
            val cacheKey = cache.buildHeadlinesKey(country, category, sources, query, pageSize, page)
            cache.getCachedArticles(cacheKey)?.let { articles ->
                logger.d { "Returning cached headlines" }
                return ApiResponse.Success(articles)
            }

            // Check rate limit
            if (!cache.canMakeRequest("headlines")) {
                val retryAfter = cache.getRetryAfter("headlines")
                logger.w { "Rate limit exceeded for headlines. Retry after: $retryAfter" }
                throw ApiError.RateLimitError("Rate limit exceeded", retryAfter)
            }

            // Make API request
            val response = get<JsonObject>(
                endpoint = "top-headlines",
                params = buildMap {
                    country?.let { put("country", it) }
                    category?.let { put("category", it) }
                    sources?.let { put("sources", it) }
                    query?.let { put("q", it) }
                    pageSize?.let { put("pageSize", it.toString()) }
                    page?.let { put("page", it.toString()) }
                }
            )

            response.collect { apiResponse ->
                when (apiResponse) {
                    is ApiResponse.Success -> {
                        val articles = parseArticles(apiResponse.data)
                        cache.cacheArticles(cacheKey, articles)
                        cache.incrementRequestCount("headlines")
                        emit(ApiResponse.Success(articles))
                    }
                    is ApiResponse.Loading -> emit(ApiResponse.Loading)
                    is ApiResponse.Error -> emit(apiResponse)
                }
            }

            ApiResponse.Loading
        } catch (e: Exception) {
            logger.e(e) { "Error fetching top headlines" }
            val apiError = when (e) {
                is ApiError -> e
                else -> ApiError.UnknownError("Failed to fetch headlines", e)
            }
            ApiResponse.Error(
                message = apiError.message ?: "Failed to fetch headlines",
                code = (apiError as? ApiError.ServerError)?.code,
                cause = apiError
            )
        }
    }

    suspend fun searchNews(
        query: NewsSearchQuery,
        pageSize: Int? = null,
        page: Int? = null
    ): ApiResponse<List<NewsArticle>> {
        return try {
            logger.d { "Searching news with query: ${query.query}" }
            
            // Check cache first
            val cacheKey = cache.buildSearchKey(query.query, pageSize, page)
            cache.getCachedArticles(cacheKey)?.let { articles ->
                logger.d { "Returning cached search results" }
                return ApiResponse.Success(articles)
            }

            // Check rate limit
            if (!cache.canMakeRequest("search")) {
                val retryAfter = cache.getRetryAfter("search")
                logger.w { "Rate limit exceeded for search. Retry after: $retryAfter" }
                throw ApiError.RateLimitError("Rate limit exceeded", retryAfter)
            }

            // Make API request
            val response = get<JsonObject>(
                endpoint = "everything",
                params = buildMap {
                    put("q", query.query)
                    query.searchIn?.let { put("searchIn", it) }
                    query.sources?.let { put("sources", it.joinToString(",")) }
                    query.domains?.let { put("domains", it.joinToString(",")) }
                    query.excludeDomains?.let { put("excludeDomains", it.joinToString(",")) }
                    query.from?.let { put("from", it) }
                    query.to?.let { put("to", it) }
                    query.language?.let { put("language", it) }
                    query.sortBy?.let { put("sortBy", it.toApiValue()) }
                    pageSize?.let { put("pageSize", it.toString()) }
                    page?.let { put("page", it.toString()) }
                }
            )

            response.collect { apiResponse ->
                when (apiResponse) {
                    is ApiResponse.Success -> {
                        val articles = parseArticles(apiResponse.data)
                        cache.cacheArticles(cacheKey, articles)
                        cache.incrementRequestCount("search")
                        emit(ApiResponse.Success(articles))
                    }
                    is ApiResponse.Loading -> emit(ApiResponse.Loading)
                    is ApiResponse.Error -> emit(apiResponse)
                }
            }

            ApiResponse.Loading
        } catch (e: Exception) {
            logger.e(e) { "Error searching news" }
            val apiError = when (e) {
                is ApiError -> e
                else -> ApiError.UnknownError("Failed to search news", e)
            }
            ApiResponse.Error(
                message = apiError.message ?: "Failed to search news",
                code = (apiError as? ApiError.ServerError)?.code,
                cause = apiError
            )
        }
    }

    suspend fun getSources(
        category: String? = null,
        language: String? = null,
        country: String? = null
    ): ApiResponse<List<NewsSource>> {
        return try {
            logger.d { "Fetching news sources" }
            
            // Check cache first
            val cacheKey = cache.buildSourcesKey(category, language, country)
            cache.getCachedSources(cacheKey)?.let { sources ->
                logger.d { "Returning cached sources" }
                return ApiResponse.Success(sources)
            }

            // Check rate limit
            if (!cache.canMakeRequest("sources")) {
                val retryAfter = cache.getRetryAfter("sources")
                logger.w { "Rate limit exceeded for sources. Retry after: $retryAfter" }
                throw ApiError.RateLimitError("Rate limit exceeded", retryAfter)
            }

            // Make API request
            val response = get<JsonObject>(
                endpoint = "sources",
                params = buildMap {
                    category?.let { put("category", it) }
                    language?.let { put("language", it) }
                    country?.let { put("country", it) }
                }
            )

            response.collect { apiResponse ->
                when (apiResponse) {
                    is ApiResponse.Success -> {
                        val sources = parseSources(apiResponse.data)
                        cache.cacheSources(cacheKey, sources)
                        cache.incrementRequestCount("sources")
                        emit(ApiResponse.Success(sources))
                    }
                    is ApiResponse.Loading -> emit(ApiResponse.Loading)
                    is ApiResponse.Error -> emit(apiResponse)
                }
            }

            ApiResponse.Loading
        } catch (e: Exception) {
            logger.e(e) { "Error fetching sources" }
            val apiError = when (e) {
                is ApiError -> e
                else -> ApiError.UnknownError("Failed to fetch sources", e)
            }
            ApiResponse.Error(
                message = apiError.message ?: "Failed to fetch sources",
                code = (apiError as? ApiError.ServerError)?.code,
                cause = apiError
            )
        }
    }

    private fun parseArticles(json: JsonObject): List<NewsArticle> {
        return json["articles"]?.jsonArray?.mapNotNull { article ->
            try {
                val articleObj = article.jsonObject
                NewsArticle(
                    source = articleObj["source"]?.jsonObject?.let { source ->
                        NewsSource(
                            id = source["id"]?.jsonPrimitive?.contentOrNull,
                            name = source["name"]?.jsonPrimitive?.content ?: return@mapNotNull null,
                            description = null,
                            url = null,
                            category = null,
                            language = null,
                            country = null
                        )
                    } ?: return@mapNotNull null,
                    author = articleObj["author"]?.jsonPrimitive?.contentOrNull,
                    title = articleObj["title"]?.jsonPrimitive?.content ?: return@mapNotNull null,
                    description = articleObj["description"]?.jsonPrimitive?.contentOrNull,
                    url = articleObj["url"]?.jsonPrimitive?.content ?: return@mapNotNull null,
                    urlToImage = articleObj["urlToImage"]?.jsonPrimitive?.contentOrNull,
                    publishedAt = articleObj["publishedAt"]?.jsonPrimitive?.content?.let { 
                        try {
                            Instant.parse(it)
                        } catch (e: Exception) {
                            logger.w(e) { "Failed to parse date: $it" }
                            null
                        }
                    } ?: return@mapNotNull null,
                    content = articleObj["content"]?.jsonPrimitive?.contentOrNull
                )
            } catch (e: Exception) {
                logger.w(e) { "Failed to parse article" }
                null
            }
        } ?: emptyList()
    }

    private fun parseSources(json: JsonObject): List<NewsSource> {
        return json["sources"]?.jsonArray?.mapNotNull { source ->
            try {
                val sourceObj = source.jsonObject
                NewsSource(
                    id = sourceObj["id"]?.jsonPrimitive?.contentOrNull,
                    name = sourceObj["name"]?.jsonPrimitive?.content ?: return@mapNotNull null,
                    description = sourceObj["description"]?.jsonPrimitive?.contentOrNull,
                    url = sourceObj["url"]?.jsonPrimitive?.contentOrNull,
                    category = sourceObj["category"]?.jsonPrimitive?.contentOrNull,
                    language = sourceObj["language"]?.jsonPrimitive?.contentOrNull,
                    country = sourceObj["country"]?.jsonPrimitive?.contentOrNull
                )
            } catch (e: Exception) {
                logger.w(e) { "Failed to parse source" }
                null
            }
        } ?: emptyList()
    }

    private class NewsCache {
        private val articleCache = mutableMapOf<String, CacheEntry<List<NewsArticle>>>()
        private val sourceCache = mutableMapOf<String, CacheEntry<List<NewsSource>>>()
        private val requestCounts = mutableMapOf<String, RequestCount>()

        fun getCachedArticles(key: String): List<NewsArticle>? {
            val entry = articleCache[key] ?: return null
            if (entry.isExpired()) {
                articleCache.remove(key)
                return null
            }
            return entry.data
        }

        fun getCachedSources(key: String): List<NewsSource>? {
            val entry = sourceCache[key] ?: return null
            if (entry.isExpired()) {
                sourceCache.remove(key)
                return null
            }
            return entry.data
        }

        fun cacheArticles(key: String, articles: List<NewsArticle>) {
            articleCache[key] = CacheEntry(articles)
        }

        fun cacheSources(key: String, sources: List<NewsSource>) {
            sourceCache[key] = CacheEntry(sources)
        }

        fun canMakeRequest(type: String): Boolean {
            val count = requestCounts[type]
            return count == null || !count.isLimited()
        }

        fun getRetryAfter(type: String): Duration {
            val count = requestCounts[type] ?: return Duration.ZERO
            return count.getRetryAfter()
        }

        fun incrementRequestCount(type: String) {
            val count = requestCounts[type] ?: RequestCount()
            count.increment()
            requestCounts[type] = count
        }

        fun buildHeadlinesKey(
            country: String?,
            category: String?,
            sources: String?,
            query: String?,
            pageSize: Int?,
            page: Int?
        ): String = buildString {
            append("headlines:")
            append(country ?: "")
            append(":")
            append(category ?: "")
            append(":")
            append(sources ?: "")
            append(":")
            append(query ?: "")
            append(":")
            append(pageSize ?: "")
            append(":")
            append(page ?: "")
        }

        fun buildSearchKey(query: String, pageSize: Int?, page: Int?): String =
            "search:$query:${pageSize ?: ""}:${page ?: ""}"

        fun buildSourcesKey(category: String?, language: String?, country: String?): String =
            "sources:${category ?: ""}:${language ?: ""}:${country ?: ""}"

        private data class CacheEntry<T>(
            val data: T,
            val timestamp: Instant = Clock.System.now()
        ) {
            fun isExpired(): Boolean =
                Clock.System.now() - timestamp > 5.minutes
        }

        private data class RequestCount(
            var count: Int = 0,
            var lastReset: Instant = Clock.System.now()
        ) {
            fun increment() {
                val now = Clock.System.now()
                if (now - lastReset > 1.minutes) {
                    count = 1
                    lastReset = now
                } else {
                    count++
                }
            }

            fun isLimited(): Boolean =
                count >= 30 && Clock.System.now() - lastReset <= 1.minutes

            fun getRetryAfter(): Duration {
                val now = Clock.System.now()
                val timeSinceReset = now - lastReset
                return if (timeSinceReset >= 1.minutes) {
                    Duration.ZERO
                } else {
                    1.minutes - timeSinceReset
                }
            }
        }
    }
}
