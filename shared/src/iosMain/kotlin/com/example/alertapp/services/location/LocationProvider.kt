package com.example.alertapp.services.location

import com.example.alertapp.models.weather.WeatherLocation
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import platform.CoreLocation.*
import platform.Foundation.NSError

actual class LocationProvider {
    companion object {
        actual val LOCATION_UPDATE_INTERVAL: Long = 60_000L // 1 minute
        actual val LOCATION_FASTEST_INTERVAL: Long = 30_000L // 30 seconds
        actual val LOCATION_SMALLEST_DISPLACEMENT: Float = 100f // 100 meters
    }

    private val locationManager = CLLocationManager()

    actual suspend fun getCoordinates(): Flow<WeatherLocation> = callbackFlow {
        val delegate = object : NSObject(), CLLocationManagerDelegateProtocol {
            override fun locationManager(manager: CLLocationManager, didUpdateLocations: List<*>) {
                (didUpdateLocations.lastOrNull() as? CLLocation)?.let { location ->
                    trySend(location.toWeatherLocation())
                }
            }

            override fun locationManager(manager: CLLocationManager, didFailWithError: NSError) {
                // Handle error if needed
            }
        }

        locationManager.delegate = delegate
        locationManager.desiredAccuracy = kCLLocationAccuracyBest
        locationManager.distanceFilter = LOCATION_SMALLEST_DISPLACEMENT.toDouble()
        locationManager.startUpdatingLocation()

        awaitClose {
            locationManager.stopUpdatingLocation()
        }
    }

    private fun CLLocation.toWeatherLocation() = WeatherLocation(
        latitude = coordinate.latitude,
        longitude = coordinate.longitude
    )
}
