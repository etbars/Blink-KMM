package com.example.alertapp.location

/**
 * Platform-agnostic interface for location services.
 */
interface LocationService {
    /**
     * Converts a location name to coordinates.
     * @param location The location name to geocode (e.g., "New York, USA")
     * @return The coordinates for the location, or null if not found
     */
    suspend fun getCoordinates(location: String): Coordinates?

    /**
     * Converts coordinates to a location name.
     * @param latitude The latitude coordinate
     * @param longitude The longitude coordinate
     * @return A formatted location name, or null if not found
     */
    suspend fun getLocationName(latitude: Double, longitude: Double): String?

    /**
     * Gets the formatted address components for coordinates.
     * @param latitude The latitude coordinate
     * @param longitude The longitude coordinate
     * @return A map of address components, or empty if not found
     */
    suspend fun getAddressComponents(
        latitude: Double, 
        longitude: Double
    ): Map<AddressComponent, String>

    /**
     * Validates if a location string is valid and can be geocoded.
     * @param location The location string to validate
     * @return True if the location is valid and can be geocoded
     */
    suspend fun isValidLocation(location: String): Boolean
}

/**
 * Address components that can be returned by the location service.
 */
enum class AddressComponent {
    STREET,
    CITY,
    STATE,
    POSTAL_CODE,
    COUNTRY,
    COUNTRY_CODE
}
