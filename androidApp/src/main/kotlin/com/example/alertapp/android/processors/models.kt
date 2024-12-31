package com.example.alertapp.android.processors

data class WeatherData(
    val temperature: Double,
    val windSpeed: Double,
    val precipitation: Double,
    val snowfall: Double,
    val humidity: Double,
    val pressure: Double,
    val description: String
)

data class PriceData(
    val symbol: String,
    val currentPrice: Double,
    val openPrice: Double,
    val highPrice: Double,
    val lowPrice: Double,
    val volume: Long,
    val averageVolume: Long,
    val timestamp: Long
)

data class ContentData(
    val url: String,
    val text: String,
    val statusCode: Int,
    val lastModified: Long,
    val contentType: String,
    val headers: Map<String, String>
)
