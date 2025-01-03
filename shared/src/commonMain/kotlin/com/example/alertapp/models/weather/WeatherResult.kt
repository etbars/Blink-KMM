package com.example.alertapp.models.weather

sealed class WeatherResult {
    data class Success(val data: WeatherData) : WeatherResult()
    data class Error(val message: String) : WeatherResult()
}
