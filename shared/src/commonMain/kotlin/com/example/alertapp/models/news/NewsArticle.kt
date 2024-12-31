package com.example.alertapp.models.news

import kotlinx.serialization.Serializable
import kotlinx.datetime.Instant

/**
 * Represents a news article from the API.
 */
@Serializable
data class NewsArticle(
    val id: String,
    val title: String,
    val description: String? = null,
    val content: String? = null,
    val url: String,
    val imageUrl: String? = null,
    val source: NewsSource,
    val publishedAt: Instant,
    val category: NewsCategory? = null
) {
    fun containsKeyword(keyword: String): Boolean {
        val normalizedKeyword = keyword.lowercase()
        return title.lowercase().contains(normalizedKeyword) ||
               description?.lowercase()?.contains(normalizedKeyword) == true ||
               content?.lowercase()?.contains(normalizedKeyword) == true
    }

    fun containsAllKeywords(keywords: Set<String>): Boolean {
        return keywords.all { containsKeyword(it) }
    }

    fun containsAnyKeyword(keywords: Set<String>): Boolean {
        return keywords.any { containsKeyword(it) }
    }

    fun matchesKeywords(
        keywords: Set<String>,
        excludeKeywords: Set<String> = emptySet(),
        matchAll: Boolean = false
    ): Boolean {
        if (excludeKeywords.any { containsKeyword(it) }) {
            return false
        }
        return if (matchAll) {
            containsAllKeywords(keywords)
        } else {
            containsAnyKeyword(keywords)
        }
    }

    fun getPreview(maxLength: Int = 200): String {
        val text = description ?: content ?: return title
        return if (text.length <= maxLength) {
            text
        } else {
            text.take(maxLength - 3) + "..."
        }
    }
}

@Serializable
data class NewsSource(
    val id: String,
    val name: String,
    val description: String? = null,
    val url: String? = null,
    val category: NewsCategory? = null,
    val language: String? = null,
    val country: String? = null
)

@Serializable
enum class NewsCategory {
    BUSINESS,
    ENTERTAINMENT,
    GENERAL,
    HEALTH,
    SCIENCE,
    SPORTS,
    TECHNOLOGY;

    override fun toString(): String = name.lowercase()

    companion object {
        fun fromString(value: String): NewsCategory {
            return values().find { it.toString() == value.lowercase() }
                ?: throw IllegalArgumentException("Unknown category: $value")
        }
    }
}

@Serializable
enum class NewsSortBy {
    RELEVANCY,
    POPULARITY,
    PUBLISHED_AT;

    override fun toString(): String = name.lowercase()

    companion object {
        fun fromString(value: String): NewsSortBy {
            return values().find { it.toString() == value.lowercase() }
                ?: throw IllegalArgumentException("Unknown sort by: $value")
        }
    }
}
