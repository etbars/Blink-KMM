package com.example.alertapp.api.content.utils

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class KeywordMatcher @Inject constructor() {
    fun matchesKeywords(
        text: String,
        keywords: List<String>,
        mustIncludeAll: Boolean,
        excludeKeywords: List<String> = emptyList()
    ): Boolean {
        if (keywords.isEmpty()) return true
        
        val textLower = text.lowercase()
        val keywordsLower = keywords.map { it.lowercase() }
        val excludeKeywordsLower = excludeKeywords.map { it.lowercase() }

        // Check if any exclude keywords are present
        if (excludeKeywordsLower.any { textLower.contains(it) }) {
            return false
        }

        // Check if keywords are present based on mustIncludeAll flag
        return if (mustIncludeAll) {
            keywordsLower.all { keyword -> textLower.contains(keyword) }
        } else {
            keywordsLower.any { keyword -> textLower.contains(keyword) }
        }
    }
}
