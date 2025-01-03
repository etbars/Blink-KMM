package com.example.alertapp.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class ContentSource {
    @SerialName("news")
    NEWS,
    @SerialName("blog")
    BLOG,
    @SerialName("social_media")
    SOCIAL_MEDIA,
    @SerialName("rss")
    RSS,
    @SerialName("website")
    WEBSITE;

    companion object {
        fun fromString(value: String): ContentSource {
            return try {
                valueOf(value.uppercase())
            } catch (e: IllegalArgumentException) {
                NEWS // Default to NEWS if unknown source
            }
        }

        fun toString(source: ContentSource): String {
            return when (source) {
                NEWS -> "news"
                BLOG -> "blog"
                SOCIAL_MEDIA -> "social_media"
                RSS -> "rss"
                WEBSITE -> "website"
            }
        }
    }
}
