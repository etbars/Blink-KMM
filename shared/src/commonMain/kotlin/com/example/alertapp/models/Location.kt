package com.example.alertapp.models

import kotlinx.serialization.Serializable

@Serializable
data class Location(
    val latitude: Double,
    val longitude: Double,
    val name: String? = null
)
