package com.example.alertapp.services.processors

import android.content.Context
import android.location.Geocoder
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
actual class LocationProvider @Inject constructor(
    private val context: Context
) {
    actual suspend fun getCoordinates(location: String): Coordinates? {
        return try {
            val geocoder = Geocoder(context, Locale.getDefault())
            val addresses = geocoder.getFromLocationName(location, 1)
            
            if (!addresses.isNullOrEmpty()) {
                val address = addresses[0]
                Coordinates(
                    latitude = address.latitude,
                    longitude = address.longitude
                )
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
}
