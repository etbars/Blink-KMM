package com.example.alertapp.location

import kotlin.math.*

/**
 * Utility functions for location-related calculations and formatting.
 */
object LocationUtils {
    /**
     * Earth's radius in kilometers, used for distance calculations.
     */
    const val EARTH_RADIUS_KM = 6371.0
    
    /**
     * Earth's radius in miles, used for distance calculations.
     */
    const val EARTH_RADIUS_MILES = 3959.0

    /**
     * Distance units for calculations.
     */
    enum class DistanceUnit {
        KILOMETERS,
        MILES,
        METERS,
        FEET
    }

    /**
     * Calculate the distance between two coordinates using the Haversine formula.
     * @param start Starting coordinates
     * @param end Ending coordinates
     * @param unit Desired distance unit (default: KILOMETERS)
     * @return Distance between the points in the specified unit
     */
    fun calculateDistance(
        start: Coordinates,
        end: Coordinates,
        unit: DistanceUnit = DistanceUnit.KILOMETERS
    ): Double {
        val lat1 = start.latitude.toRadians()
        val lon1 = start.longitude.toRadians()
        val lat2 = end.latitude.toRadians()
        val lon2 = end.longitude.toRadians()

        val dLat = lat2 - lat1
        val dLon = lon2 - lon1

        val a = sin(dLat / 2).pow(2) + cos(lat1) * cos(lat2) * sin(dLon / 2).pow(2)
        val c = 2 * asin(sqrt(a))

        val baseDistance = when (unit) {
            DistanceUnit.KILOMETERS -> EARTH_RADIUS_KM * c
            DistanceUnit.MILES -> EARTH_RADIUS_MILES * c
            DistanceUnit.METERS -> EARTH_RADIUS_KM * c * 1000
            DistanceUnit.FEET -> EARTH_RADIUS_MILES * c * 5280
        }

        return round(baseDistance * 100) / 100 // Round to 2 decimal places
    }

    /**
     * Check if a coordinate is within a radius of another coordinate.
     * @param center Center coordinates
     * @param point Point to check
     * @param radius Radius to check within
     * @param unit Unit of the radius
     * @return True if the point is within the radius
     */
    fun isWithinRadius(
        center: Coordinates,
        point: Coordinates,
        radius: Double,
        unit: DistanceUnit = DistanceUnit.KILOMETERS
    ): Boolean = calculateDistance(center, point, unit) <= radius

    /**
     * Format coordinates as a string.
     * @param coordinates The coordinates to format
     * @param format The desired format (DMS = Degrees Minutes Seconds, DD = Decimal Degrees)
     * @return Formatted coordinate string
     */
    fun formatCoordinates(
        coordinates: Coordinates,
        format: CoordinateFormat = CoordinateFormat.DD
    ): String = when (format) {
        CoordinateFormat.DD -> formatDD(coordinates)
        CoordinateFormat.DMS -> formatDMS(coordinates)
    }

    /**
     * Calculate the center point between multiple coordinates.
     * @param points List of coordinates
     * @return The center (centroid) coordinates
     */
    fun calculateCenter(points: List<Coordinates>): Coordinates {
        if (points.isEmpty()) return Coordinates.INVALID
        if (points.size == 1) return points.first()

        var x = 0.0
        var y = 0.0
        var z = 0.0

        points.forEach { point ->
            val lat = point.latitude.toRadians()
            val lon = point.longitude.toRadians()

            x += cos(lat) * cos(lon)
            y += cos(lat) * sin(lon)
            z += sin(lat)
        }

        val total = points.size

        x /= total
        y /= total
        z /= total

        val centralLongitude = atan2(y, x)
        val centralSquareRoot = sqrt(x * x + y * y)
        val centralLatitude = atan2(z, centralSquareRoot)

        return Coordinates(
            latitude = centralLatitude.toDegrees(),
            longitude = centralLongitude.toDegrees()
        )
    }

    /**
     * Get the bounding box for a set of coordinates.
     * @param points List of coordinates
     * @return A pair of coordinates representing the southwest and northeast corners
     */
    fun getBoundingBox(points: List<Coordinates>): Pair<Coordinates, Coordinates> {
        if (points.isEmpty()) return Coordinates.INVALID to Coordinates.INVALID
        if (points.size == 1) return points.first() to points.first()

        var minLat = 90.0
        var maxLat = -90.0
        var minLon = 180.0
        var maxLon = -180.0

        points.forEach { point ->
            minLat = minOf(minLat, point.latitude)
            maxLat = maxOf(maxLat, point.latitude)
            minLon = minOf(minLon, point.longitude)
            maxLon = maxOf(maxLon, point.longitude)
        }

        return Coordinates(minLat, minLon) to Coordinates(maxLat, maxLon)
    }

    /**
     * Format for coordinate display.
     */
    enum class CoordinateFormat {
        DD,  // Decimal Degrees (e.g., 40.7128°N, 74.0060°W)
        DMS  // Degrees Minutes Seconds (e.g., 40°42'51"N, 74°00'21"W)
    }

    private fun Double.toRadians(): Double = this * PI / 180.0
    private fun Double.toDegrees(): Double = this * 180.0 / PI

    private fun formatDD(coordinates: Coordinates): String {
        val latDir = if (coordinates.latitude >= 0) "N" else "S"
        val lonDir = if (coordinates.longitude >= 0) "E" else "W"
        return "${abs(coordinates.latitude).format(4)}°$latDir, ${abs(coordinates.longitude).format(4)}°$lonDir"
    }

    private fun formatDMS(coordinates: Coordinates): String {
        fun convertToDMS(decimal: Double): Triple<Int, Int, Double> {
            val degrees = decimal.toInt()
            val minutesDecimal = (decimal - degrees) * 60
            val minutes = minutesDecimal.toInt()
            val seconds = (minutesDecimal - minutes) * 60
            return Triple(abs(degrees), minutes, seconds)
        }

        val (latDeg, latMin, latSec) = convertToDMS(coordinates.latitude)
        val (lonDeg, lonMin, lonSec) = convertToDMS(coordinates.longitude)
        val latDir = if (coordinates.latitude >= 0) "N" else "S"
        val lonDir = if (coordinates.longitude >= 0) "E" else "W"

        return "$latDeg°$latMin'${latSec.format(1)}\"$latDir, $lonDeg°$lonMin'${lonSec.format(1)}\"$lonDir"
    }

    private fun Double.format(decimals: Int): String {
        return "%.${decimals}f".format(this)
    }
}

expect fun String.format(vararg args: Any): String
