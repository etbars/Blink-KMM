package com.example.alertapp.models

import kotlinx.serialization.Serializable

@Serializable
enum class ReleaseCondition {
    NEW_RELEASE,
    VERSION_UPDATE,
    RATING_CHANGE,
    CATEGORY_CHANGE,
    PLATFORM_CHANGE,
    MEDIA_TYPE_CHANGE
}
