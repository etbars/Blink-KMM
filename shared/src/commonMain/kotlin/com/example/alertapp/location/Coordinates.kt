package com.example.alertapp.location

import kotlinx.serialization.Serializable

/**
 * Represents geographical coordinates with latitude and longitude.
 */
@Serializable
data class Coordinates(
    val latitude: Double,
    val longitude: Double
) {
    companion object {
        val INVALID = Coordinates(0.0, 0.0)
    }

    fun isValid(): Boolean = 
        latitude != 0.0 && longitude != 0.0 &&
        latitude >= -90.0 && latitude <= 90.0 &&
        longitude >= -180.0 && longitude <= 180.0
}
