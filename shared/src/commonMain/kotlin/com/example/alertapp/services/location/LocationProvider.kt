package com.example.alertapp.services.location

import com.example.alertapp.models.weather.WeatherLocation
import kotlinx.coroutines.flow.Flow

expect class LocationProvider {
    suspend fun getCoordinates(): Flow<WeatherLocation>
    
    companion object {
        val LOCATION_UPDATE_INTERVAL: Long
        val LOCATION_FASTEST_INTERVAL: Long
        val LOCATION_SMALLEST_DISPLACEMENT: Float
    }
}
