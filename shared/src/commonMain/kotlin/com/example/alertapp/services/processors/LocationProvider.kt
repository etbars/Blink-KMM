package com.example.alertapp.services.processors

import com.example.alertapp.models.Coordinates

expect class LocationProvider {
    suspend fun getCoordinates(location: String): Coordinates?
}
