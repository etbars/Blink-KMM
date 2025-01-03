package com.example.alertapp.services.processors

import com.example.alertapp.models.Coordinates
import platform.CoreLocation.CLGeocoder
import platform.CoreLocation.CLLocation
import platform.CoreLocation.CLPlacemark
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

actual class LocationProvider {
    private val geocoder = CLGeocoder()

    actual suspend fun getCoordinates(location: String): Coordinates? = suspendCancellableCoroutine { continuation ->
        try {
            geocoder.geocodeAddressString(location) { placemarks, error ->
                if (error != null) {
                    continuation.resumeWithException(Exception(error.localizedDescription))
                    return@geocodeAddressString
                }

                val placemark = placemarks?.firstOrNull() as? CLPlacemark
                if (placemark != null) {
                    val location = placemark.location
                    if (location != null) {
                        continuation.resume(Coordinates(
                            latitude = location.coordinate.latitude,
                            longitude = location.coordinate.longitude
                        ))
                    } else {
                        continuation.resume(null)
                    }
                } else {
                    continuation.resume(null)
                }
            }
        } catch (e: Exception) {
            continuation.resumeWithException(e)
        }
    }
}
