package com.example.alertapp.services.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.os.Looper
import com.example.alertapp.models.weather.WeatherLocation
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
actual class LocationProvider @Inject constructor(
    private val context: Context,
    private val fusedLocationClient: FusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context)
) {
    actual companion object {
        actual val LOCATION_UPDATE_INTERVAL: Long = 60_000L // 1 minute
        actual val LOCATION_FASTEST_INTERVAL: Long = 30_000L // 30 seconds
        actual val LOCATION_SMALLEST_DISPLACEMENT: Float = 100f // 100 meters
    }

    @SuppressLint("MissingPermission")
    actual suspend fun getCoordinates(): Flow<WeatherLocation> = callbackFlow {
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, LOCATION_UPDATE_INTERVAL)
            .setMinUpdateIntervalMillis(LOCATION_FASTEST_INTERVAL)
            .setMinUpdateDistanceMeters(LOCATION_SMALLEST_DISPLACEMENT)
            .build()

        val locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { location ->
                    trySend(location.toWeatherLocation())
                }
            }
        }

        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )

        awaitClose {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        }
    }

    private fun Location.toWeatherLocation() = WeatherLocation(
        latitude = latitude,
        longitude = longitude
    )
}
