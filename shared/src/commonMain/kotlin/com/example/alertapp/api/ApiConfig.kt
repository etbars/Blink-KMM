package com.example.alertapp.api

interface ApiConfig {
    val baseUrl: String
    val apiKey: String?
    val timeout: Long
        get() = 30_000 // 30 seconds default timeout
    val headers: Map<String, String>
        get() = buildMap {
            apiKey?.let { put("Authorization", "Bearer $it") }
        }
}
