package com.example.alertapp.location

import com.example.alertapp.models.Coordinates
import com.example.alertapp.models.AddressComponent
import kotlinx.cinterop.ExperimentalForeignApi
import platform.CoreLocation.*
import platform.Foundation.NSObject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

@OptIn(ExperimentalForeignApi::class)
class IosLocationService : LocationService {
    private val locationManager = CLLocationManager()
    private val geocoder = CLGeocoder()

    override suspend fun getAddressComponents(latitude: Double, longitude: Double): Map<AddressComponent, String> {
        val location = CLLocation(latitude, longitude)
        return suspendCoroutine { continuation ->
            geocoder.reverseGeocodeLocation(location) { placemarks, error ->
                if (error != null) {
                    continuation.resumeWithException(Exception(error.localizedDescription))
                    return@reverseGeocodeLocation
                }
                
                val placemark = placemarks?.firstOrNull()
                if (placemark != null) {
                    val components = mutableMapOf<AddressComponent, String>()
                    placemark.thoroughfare?.let { components[AddressComponent.STREET] = it }
                    placemark.locality?.let { components[AddressComponent.CITY] = it }
                    placemark.administrativeArea?.let { components[AddressComponent.STATE] = it }
                    placemark.postalCode?.let { components[AddressComponent.POSTAL_CODE] = it }
                    placemark.country?.let { components[AddressComponent.COUNTRY] = it }
                    placemark.ISOcountryCode?.let { components[AddressComponent.COUNTRY_CODE] = it }
                    continuation.resume(components)
                } else {
                    continuation.resume(emptyMap())
                }
            }
        }
    }

    override suspend fun getLocationName(latitude: Double, longitude: Double): String? {
        val location = CLLocation(latitude, longitude)
        return suspendCoroutine { continuation ->
            geocoder.reverseGeocodeLocation(location) { placemarks, error ->
                if (error != null) {
                    continuation.resumeWithException(Exception(error.localizedDescription))
                    return@reverseGeocodeLocation
                }
                
                val placemark = placemarks?.firstOrNull()
                if (placemark != null) {
                    val locality = placemark.locality
                    val country = placemark.country
                    continuation.resume(buildString {
                        locality?.let { append(it) }
                        country?.let { 
                            if (isNotEmpty()) append(", ")
                            append(it)
                        }
                    }.takeIf { it.isNotEmpty() })
                } else {
                    continuation.resume(null)
                }
            }
        }
    }

    override suspend fun getCoordinates(location: String): Coordinates? {
        return suspendCoroutine { continuation ->
            geocoder.geocodeAddressString(location) { placemarks, error ->
                if (error != null) {
                    continuation.resumeWithException(Exception(error.localizedDescription))
                    return@geocodeAddressString
                }
                
                val placemark = placemarks?.firstOrNull()
                if (placemark != null) {
                    continuation.resume(Coordinates(
                        latitude = placemark.location?.coordinate?.latitude ?: 0.0,
                        longitude = placemark.location?.coordinate?.longitude ?: 0.0
                    ))
                } else {
                    continuation.resume(null)
                }
            }
        }
    }

    override suspend fun isValidLocation(location: String): Boolean {
        return try {
            val coordinates = getCoordinates(location)
            coordinates != null
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun getCurrentLocation(): Coordinates {
        return suspendCoroutine { continuation ->
            val delegate = object : NSObject(), CLLocationManagerDelegate {
                override fun locationManager(manager: CLLocationManager, didUpdateLocations: List<*>) {
                    val locations = didUpdateLocations.filterIsInstance<CLLocation>()
                    if (locations.isNotEmpty()) {
                        val location = locations.first()
                        continuation.resume(Coordinates(
                            latitude = location.coordinate.latitude,
                            longitude = location.coordinate.longitude
                        ))
                        locationManager.stopUpdatingLocation()
                        locationManager.delegate = null
                    }
                }

                override fun locationManager(manager: CLLocationManager, didFailWithError: NSError) {
                    continuation.resumeWithException(Exception(didFailWithError.localizedDescription))
                    locationManager.stopUpdatingLocation()
                    locationManager.delegate = null
                }
            }

            locationManager.delegate = delegate
            locationManager.requestWhenInUseAuthorization()
            locationManager.startUpdatingLocation()
        }
    }

    fun startLocationUpdates(onLocationUpdate: (Coordinates) -> Unit) {
        val delegate = object : NSObject(), CLLocationManagerDelegate {
            override fun locationManager(manager: CLLocationManager, didUpdateLocations: List<*>) {
                val locations = didUpdateLocations.filterIsInstance<CLLocation>()
                if (locations.isNotEmpty()) {
                    val location = locations.first()
                    onLocationUpdate(Coordinates(
                        latitude = location.coordinate.latitude,
                        longitude = location.coordinate.longitude
                    ))
                }
            }

            override fun locationManager(manager: CLLocationManager, didFailWithError: NSError) {
                // Handle error but continue updates
                println("Location update error: ${didFailWithError.localizedDescription}")
            }
        }

        locationManager.delegate = delegate
        locationManager.requestWhenInUseAuthorization()
        locationManager.startUpdatingLocation()
    }

    fun stopLocationUpdates() {
        locationManager.stopUpdatingLocation()
        locationManager.delegate = null
    }
}
