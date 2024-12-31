package com.example.alertapp.models

import kotlinx.serialization.Serializable

@Serializable
enum class WeatherMetric {
    TEMPERATURE,
    FEELS_LIKE,
    HUMIDITY,
    PRESSURE,
    WIND_SPEED,
    RAINFALL,
    CLOUDINESS,
    UV_INDEX,
    AIR_QUALITY;

    fun getDisplayName(): String {
        return when (this) {
            TEMPERATURE -> "Temperature"
            FEELS_LIKE -> "Feels Like"
            HUMIDITY -> "Humidity"
            PRESSURE -> "Pressure"
            WIND_SPEED -> "Wind Speed"
            RAINFALL -> "Rainfall"
            CLOUDINESS -> "Cloudiness"
            UV_INDEX -> "UV Index"
            AIR_QUALITY -> "Air Quality"
        }
    }

    fun getDefaultThreshold(): Double {
        return when (this) {
            TEMPERATURE -> 25.0 // 25°C
            FEELS_LIKE -> 25.0 // 25°C
            HUMIDITY -> 70.0 // 70%
            PRESSURE -> 1013.0 // 1013 hPa (standard atmospheric pressure)
            WIND_SPEED -> 10.0 // 10 m/s
            RAINFALL -> 5.0 // 5mm
            CLOUDINESS -> 75.0 // 75%
            UV_INDEX -> 6.0 // 6 UV index
            AIR_QUALITY -> 100.0 // 100 AQI
        }
    }

    fun getValueRange(): ClosedRange<Double> {
        return when (this) {
            TEMPERATURE, FEELS_LIKE -> -50.0..60.0
            HUMIDITY, CLOUDINESS -> 0.0..100.0
            PRESSURE -> 870.0..1085.0
            WIND_SPEED -> 0.0..200.0
            RAINFALL -> 0.0..500.0
            UV_INDEX -> 0.0..12.0
            AIR_QUALITY -> 0.0..500.0
        }
    }

    fun getUnit(): String {
        return when (this) {
            TEMPERATURE, FEELS_LIKE -> "°C"
            HUMIDITY, CLOUDINESS -> "%"
            PRESSURE -> "hPa"
            WIND_SPEED -> "m/s"
            RAINFALL -> "mm"
            UV_INDEX -> ""
            AIR_QUALITY -> "AQI"
        }
    }
}

@Serializable
enum class WeatherOperator {
    ABOVE,
    BELOW,
    EQUALS,
    NOT_EQUALS;

    fun getDisplayName(): String {
        return when (this) {
            ABOVE -> "is above"
            BELOW -> "is below"
            EQUALS -> "equals"
            NOT_EQUALS -> "is not"
        }
    }

    fun getSymbol(): String {
        return when (this) {
            ABOVE -> ">"
            BELOW -> "<"
            EQUALS -> "="
            NOT_EQUALS -> "≠"
        }
    }
}
