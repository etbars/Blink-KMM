package com.example.alertapp.enums

import kotlinx.serialization.Serializable

@Serializable
enum class Sentiment {
    VERY_NEGATIVE,
    NEGATIVE,
    NEUTRAL,
    POSITIVE,
    VERY_POSITIVE;

    companion object {
        fun fromValue(value: Double): Sentiment = when {
            value <= -0.8 -> VERY_NEGATIVE
            value <= -0.3 -> NEGATIVE
            value <= 0.3 -> NEUTRAL
            value <= 0.8 -> POSITIVE
            else -> VERY_POSITIVE
        }
    }

    fun toValue(): Double = when(this) {
        VERY_NEGATIVE -> -1.0
        NEGATIVE -> -0.5
        NEUTRAL -> 0.0
        POSITIVE -> 0.5
        VERY_POSITIVE -> 1.0
    }
}
