package com.example.alertapp.models.content

sealed class ContentResult {
    data class Success(
        val items: List<ContentItem>,
        val metadata: Map<String, String> = emptyMap()
    ) : ContentResult()

    data class Error(
        val message: String,
        val code: String? = null,
        val details: Map<String, String> = emptyMap()
    ) : ContentResult()

    object Loading : ContentResult()
}
