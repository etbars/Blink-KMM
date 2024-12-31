package com.example.alertapp.models.price

import kotlinx.serialization.Serializable
import kotlinx.datetime.Instant

@Serializable
data class OHLCV(
    val timestamp: Instant,
    val open: Double,
    val high: Double,
    val low: Double,
    val close: Double,
    val volume: Long,
    val adjustedClose: Double? = null
) {
    companion object {
        fun fromMap(map: Map<String, String>): OHLCV {
            return OHLCV(
                timestamp = Instant.parse(map.getValue("timestamp")),
                open = map.getValue("open").toDouble(),
                high = map.getValue("high").toDouble(),
                low = map.getValue("low").toDouble(),
                close = map.getValue("close").toDouble(),
                volume = map.getValue("volume").toLong(),
                adjustedClose = map["adjusted_close"]?.toDouble()
            )
        }
    }
}
