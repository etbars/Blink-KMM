package com.example.alertapp.models.price

import kotlinx.serialization.Serializable
import kotlinx.datetime.Instant

/**
 * Represents price-related data from the API.
 */
@Serializable
sealed class PriceData {
    /**
     * Represents a real-time quote for a symbol.
     */
    @Serializable
    data class Quote(
        val symbol: String,
        val currentPrice: Double,
        val priceChange: Double,
        val percentChange: Double,
        val volume: Long,
        val timestamp: Instant
    ) : PriceData()

    /**
     * Represents historical price data for a symbol.
     */
    @Serializable
    data class Historical(
        val symbol: String,
        val interval: PriceInterval,
        val prices: List<PricePoint>,
        val startTime: Instant,
        val endTime: Instant
    ) : PriceData()
}

/**
 * Represents a single price point in time.
 */
@Serializable
data class PricePoint(
    val timestamp: Instant,
    val open: Double,
    val high: Double,
    val low: Double,
    val close: Double,
    val volume: Long
) {
    val averagePrice: Double
        get() = (high + low) / 2.0

    val priceRange: Double
        get() = high - low

    val isGreen: Boolean
        get() = close >= open

    val candleSize: Double
        get() = kotlin.math.abs(close - open)

    val upperShadow: Double
        get() = if (isGreen) high - close else high - open

    val lowerShadow: Double
        get() = if (isGreen) open - low else close - low
}

@Serializable
enum class PriceInterval {
    MINUTE_1,
    MINUTE_5,
    MINUTE_15,
    MINUTE_30,
    HOUR_1,
    HOUR_4,
    DAY_1,
    WEEK_1,
    MONTH_1;

    fun toApiString(): String = when (this) {
        MINUTE_1 -> "1min"
        MINUTE_5 -> "5min"
        MINUTE_15 -> "15min"
        MINUTE_30 -> "30min"
        HOUR_1 -> "1h"
        HOUR_4 -> "4h"
        DAY_1 -> "1d"
        WEEK_1 -> "1w"
        MONTH_1 -> "1m"
    }

    companion object {
        fun fromString(str: String): PriceInterval = when (str.lowercase()) {
            "1min" -> MINUTE_1
            "5min" -> MINUTE_5
            "15min" -> MINUTE_15
            "30min" -> MINUTE_30
            "1h" -> HOUR_1
            "4h" -> HOUR_4
            "1d" -> DAY_1
            "1w" -> WEEK_1
            "1m" -> MONTH_1
            else -> throw IllegalArgumentException("Unknown interval: $str")
        }
    }
}
