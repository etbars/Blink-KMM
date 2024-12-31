package com.example.alertapp.location

import android.content.Context
import android.location.Geocoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale
import javax.inject.Inject

/**
 * Android-specific implementation of LocationService using Android's Geocoder.
 */
class AndroidLocationService @Inject constructor(
    private val context: Context
) : LocationService {
    
    override suspend fun getCoordinates(location: String): Coordinates? =
        withContext(Dispatchers.IO) {
            try {
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

    override suspend fun getLocationName(latitude: Double, longitude: Double): String? =
        withContext(Dispatchers.IO) {
            try {
                val geocoder = Geocoder(context, Locale.getDefault())
                val addresses = geocoder.getFromLocation(latitude, longitude, 1)
                
                if (!addresses.isNullOrEmpty()) {
                    val address = addresses[0]
                    buildString {
                        address.locality?.let { append(it) }
                        address.adminArea?.let { 
                            if (isNotEmpty()) append(", ")
                            append(it)
                        }
                        address.countryName?.let {
                            if (isNotEmpty()) append(", ")
                            append(it)
                        }
                    }
                } else {
                    null
                }
            } catch (e: Exception) {
                null
            }
        }

    override suspend fun getAddressComponents(
        latitude: Double,
        longitude: Double
    ): Map<AddressComponent, String> = withContext(Dispatchers.IO) {
        try {
            val geocoder = Geocoder(context, Locale.getDefault())
            val addresses = geocoder.getFromLocation(latitude, longitude, 1)
            
            if (!addresses.isNullOrEmpty()) {
                val address = addresses[0]
                buildMap {
                    address.thoroughfare?.let { put(AddressComponent.STREET, it) }
                    address.locality?.let { put(AddressComponent.CITY, it) }
                    address.adminArea?.let { put(AddressComponent.STATE, it) }
                    address.postalCode?.let { put(AddressComponent.POSTAL_CODE, it) }
                    address.countryName?.let { put(AddressComponent.COUNTRY, it) }
                    address.countryCode?.let { put(AddressComponent.COUNTRY_CODE, it) }
                }
            } else {
                emptyMap()
            }
        } catch (e: Exception) {
            emptyMap()
        }
    }

    override suspend fun isValidLocation(location: String): Boolean =
        withContext(Dispatchers.IO) {
            try {
                val geocoder = Geocoder(context, Locale.getDefault())
                val addresses = geocoder.getFromLocationName(location, 1)
                !addresses.isNullOrEmpty()
            } catch (e: Exception) {
                false
            }
        }
}
