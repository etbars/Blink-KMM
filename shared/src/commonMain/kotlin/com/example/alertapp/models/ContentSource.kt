package com.example.alertapp.models

import kotlinx.serialization.Serializable

@Serializable
enum class ContentSource {
    NEWS_API,
    REDDIT,
    TWITTER,
    RSS,
    CUSTOM
}
