package com.example.alertapp.location

import kotlinx.serialization.Serializable
import kotlin.math.*

/**
 * Represents a geographical region defined by a center point and radius.
 */
@Serializable
data class GeoRegion(
    val center: Coordinates,
    val radiusKm: Double
) {
    /**
     * Check if a point is within this region.
     */
    fun contains(point: Coordinates): Boolean =
        LocationUtils.isWithinRadius(center, point, radiusKm)

    /**
     * Get the bounding box of this region.
     * Returns a pair of coordinates representing the southwest and northeast corners.
     */
    fun getBoundingBox(): Pair<Coordinates, Coordinates> {
        // Convert radius from km to degrees (approximate)
        val latDelta = (radiusKm / LocationUtils.EARTH_RADIUS_KM) * (180.0 / PI)
        val lonDelta = abs(latDelta / cos(center.latitude.toRadians()))

        return Coordinates(
            center.latitude - latDelta,
            center.longitude - lonDelta
        ) to Coordinates(
            center.latitude + latDelta,
            center.longitude + lonDelta
        )
    }

    /**
     * Check if this region overlaps with another region.
     */
    fun overlaps(other: GeoRegion): Boolean {
        val distance = LocationUtils.calculateDistance(center, other.center)
        return distance <= (radiusKm + other.radiusKm)
    }

    /**
     * Calculate the approximate area of the region in square kilometers.
     */
    fun area(): Double = PI * radiusKm * radiusKm

    companion object {
        /**
         * Create a region that encompasses all given points with some padding.
         */
        fun fromPoints(
            points: List<Coordinates>,
            paddingKm: Double = 1.0
        ): GeoRegion {
            if (points.isEmpty()) {
                return GeoRegion(Coordinates.INVALID, 0.0)
            }
            if (points.size == 1) {
                return GeoRegion(points.first(), paddingKm)
            }

            val center = LocationUtils.calculateCenter(points)
            val maxDistance = points.maxOf { point ->
                LocationUtils.calculateDistance(center, point)
            }

            return GeoRegion(center, maxDistance + paddingKm)
        }
    }

    private fun Double.toRadians(): Double = this * PI / 180.0
}
