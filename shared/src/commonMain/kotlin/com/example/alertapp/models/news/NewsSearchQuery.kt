package com.example.alertapp.models.news

import kotlinx.serialization.Serializable
import kotlinx.datetime.Instant

@Serializable
data class NewsSearchQuery(
    val keywords: Set<String>,
    val excludeKeywords: Set<String> = emptySet(),
    val matchAll: Boolean = false,
    val sources: Set<String> = emptySet(),
    val categories: Set<NewsCategory> = emptySet(),
    val countries: Set<String> = emptySet(),
    val languages: Set<String> = emptySet(),
    val from: Instant? = null,
    val to: Instant? = null,
    val sortBy: NewsSortBy = NewsSortBy.PUBLISHED_AT,
    val pageSize: Int = 20,
    val page: Int = 1
) {
    fun toQueryMap(): Map<String, String> {
        val params = mutableMapOf<String, String>()
        
        // Keywords
        params["q"] = if (matchAll) {
            keywords.joinToString(" AND ")
        } else {
            keywords.joinToString(" OR ")
        }
        if (excludeKeywords.isNotEmpty()) {
            params["q"] += " NOT " + excludeKeywords.joinToString(" NOT ")
        }

        // Sources and categories
        if (sources.isNotEmpty()) {
            params["sources"] = sources.joinToString(",")
        }
        if (categories.isNotEmpty()) {
            params["category"] = categories.joinToString(",") { it.toString() }
        }

        // Countries and languages
        if (countries.isNotEmpty()) {
            params["country"] = countries.joinToString(",")
        }
        if (languages.isNotEmpty()) {
            params["language"] = languages.joinToString(",")
        }

        // Date range
        from?.let { params["from"] = it.toString() }
        to?.let { params["to"] = it.toString() }

        // Sorting and pagination
        params["sortBy"] = sortBy.toString()
        params["pageSize"] = pageSize.toString()
        params["page"] = page.toString()

        return params
    }
}
