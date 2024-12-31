package com.example.alertapp.services.processors

import platform.CoreLocation.*
import kotlinx.cinterop.*
import platform.Foundation.NSError
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

actual class LocationProvider {
    private val geocoder = CLGeocoder()

    actual suspend fun getCoordinates(location: String): Coordinates? = suspendCancellableCoroutine { continuation ->
        geocoder.geocodeAddressString(location) { placemarks, error ->
            if (error != null || placemarks == null) {
                continuation.resume(null)
                return@geocodeAddressString
            }

            val placemark = placemarks.firstOrNull() as? CLPlacemark
            if (placemark == null) {
                continuation.resume(null)
                return@geocodeAddressString
            }

            val location = placemark.location
            if (location == null) {
                continuation.resume(null)
                return@geocodeAddressString
            }

            continuation.resume(Coordinates(
                latitude = location.coordinate.latitude,
                longitude = location.coordinate.longitude
            ))
        }
    }
}
