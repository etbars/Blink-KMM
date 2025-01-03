package com.example.alertapp.models.content

import kotlinx.serialization.Serializable
import kotlinx.datetime.Instant
import com.example.alertapp.enums.Sentiment

@Serializable
sealed class ContentData {
    abstract val source: ContentSource
    abstract val id: String
    abstract val content: String
    abstract val keywords: Set<String>
    abstract val sentiment: Sentiment?

    @Serializable
    data class Article(
        override val id: String,
        val title: String,
        override val content: String,
        override val source: ContentSource,
        val author: String?,
        val publishedAt: Instant,
        val url: String,
        override val keywords: Set<String>,
        override val sentiment: Sentiment?,
        val relevanceScore: Double
    ) : ContentData()
    
    @Serializable
    data class SocialPost(
        override val id: String,
        val platform: Platform,
        override val source: ContentSource = ContentSource.SOCIAL,
        override val content: String,
        val author: String,
        val postedAt: Instant,
        val engagement: Engagement,
        override val keywords: Set<String>,
        override val sentiment: Sentiment?
    ) : ContentData()
}

@Serializable
data class Engagement(
    val likes: Int = 0,
    val shares: Int = 0,
    val comments: Int = 0
)

@Serializable
enum class Platform {
    TWITTER,
    FACEBOOK,
    LINKEDIN,
    REDDIT;

    override fun toString(): String = name
}

@Serializable
enum class ContentSource {
    NEWS,      // News articles from various sources
    SOCIAL,    // Social media content
    RSS,       // RSS feed sources
    CUSTOM;    // Custom content sources

    override fun toString(): String = when (this) {
        NEWS -> "News"
        SOCIAL -> "Social Media"
        RSS -> "RSS Feed"
        CUSTOM -> "Custom Source"
    }

    companion object {
        fun getDisplayName(source: ContentSource): String = when (source) {
            NEWS -> "News"
            SOCIAL -> "Social Media"
            RSS -> "RSS Feed"
            CUSTOM -> "Custom Source"
        }
    }
}
