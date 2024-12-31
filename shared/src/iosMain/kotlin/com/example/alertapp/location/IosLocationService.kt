package com.example.alertapp.location

import platform.CoreLocation.*
import platform.Foundation.*
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * iOS-specific implementation of LocationService using CoreLocation.
 */
class IosLocationService : LocationService {
    private val geocoder: CLGeocoder = CLGeocoder()
    
    override suspend fun getCoordinates(location: String): Coordinates? =
        suspendCancellableCoroutine { continuation ->
            geocoder.geocodeAddressString(location) { placemarks, error ->
                if (error != null || placemarks.isEmpty()) {
                    continuation.resume(null)
                    return@geocodeAddressString
                }
                
                val placemark = placemarks.firstOrNull() as? CLPlacemark
                val location = placemark?.location
                if (location != null) {
                    continuation.resume(Coordinates(
                        latitude = location.coordinate.latitude,
                        longitude = location.coordinate.longitude
                    ))
                } else {
                    continuation.resume(null)
                }
            }
        }

    override suspend fun getLocationName(latitude: Double, longitude: Double): String? =
        suspendCancellableCoroutine { continuation ->
            val location = CLLocation(latitude, longitude)
            geocoder.reverseGeocodeLocation(location) { placemarks, error ->
                if (error != null || placemarks.isEmpty()) {
                    continuation.resume(null)
                    return@reverseGeocodeLocation
                }
                
                val placemark = placemarks.firstOrNull() as? CLPlacemark
                if (placemark != null) {
                    val components = mutableListOf<String>()
                    placemark.locality?.let { components.add(it as String) }
                    placemark.administrativeArea?.let { components.add(it as String) }
                    placemark.country?.let { components.add(it as String) }
                    
                    continuation.resume(components.joinToString(", "))
                } else {
                    continuation.resume(null)
                }
            }
        }

    override suspend fun getAddressComponents(
        latitude: Double,
        longitude: Double
    ): Map<AddressComponent, String> = suspendCancellableCoroutine { continuation ->
        val location = CLLocation(latitude, longitude)
        geocoder.reverseGeocodeLocation(location) { placemarks, error ->
            if (error != null || placemarks.isEmpty()) {
                continuation.resume(emptyMap())
                return@reverseGeocodeLocation
            }
            
            val placemark = placemarks.firstOrNull() as? CLPlacemark
            if (placemark != null) {
                val components = buildMap {
                    placemark.thoroughfare?.let { 
                        put(AddressComponent.STREET, it as String)
                    }
                    placemark.locality?.let { 
                        put(AddressComponent.CITY, it as String)
                    }
                    placemark.administrativeArea?.let { 
                        put(AddressComponent.STATE, it as String)
                    }
                    placemark.postalCode?.let { 
                        put(AddressComponent.POSTAL_CODE, it as String)
                    }
                    placemark.country?.let { 
                        put(AddressComponent.COUNTRY, it as String)
                    }
                    placemark.ISOcountryCode?.let { 
                        put(AddressComponent.COUNTRY_CODE, it as String)
                    }
                }
                continuation.resume(components)
            } else {
                continuation.resume(emptyMap())
            }
        }
    }

    override suspend fun isValidLocation(location: String): Boolean =
        getCoordinates(location) != null
}
