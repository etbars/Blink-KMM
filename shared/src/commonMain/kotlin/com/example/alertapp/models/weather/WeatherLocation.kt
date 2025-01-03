package com.example.alertapp.models.weather

import kotlinx.serialization.Serializable

@Serializable
data class WeatherLocation(
    val latitude: Double,
    val longitude: Double,
    val name: String? = null,
    val country: String? = null,
    val state: String? = null
)
