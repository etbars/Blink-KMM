package com.example.alertapp.services.processors

import android.content.Context
import android.location.Geocoder
import com.example.alertapp.models.Coordinates
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import co.touchlab.kermit.Logger

@Singleton
actual class LocationProvider @Inject constructor(
    private val context: Context,
    private val logger: Logger = Logger.withTag("AndroidLocationProvider")
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
                logger.w("No coordinates found for location: $location")
                null
            }
        } catch (e: Exception) {
            logger.e("Error getting coordinates for location: $location", e)
            null
        }
    }
}
