package com.example.alertapp.models

import kotlinx.serialization.Serializable

@Serializable
enum class ContentType {
    NEWS,
    BLOG,
    WEATHER,
    PRICE,
    RELEASE
}
