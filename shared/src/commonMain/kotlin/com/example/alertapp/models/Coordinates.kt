package com.example.alertapp.models

import kotlinx.serialization.Serializable

@Serializable
data class Coordinates(
    val latitude: Double,
    val longitude: Double
) {
    companion object {
        val EMPTY = Coordinates(0.0, 0.0)
    }
}
