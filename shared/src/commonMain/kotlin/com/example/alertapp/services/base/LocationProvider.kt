package com.example.alertapp.services.base

import com.example.alertapp.models.Location
import com.example.alertapp.models.WeatherCondition

interface LocationProvider {
    suspend fun getCurrentLocation(): Location?
    fun parseLocation(locationStr: String): Location?
    fun parseWeatherConditions(conditionsStr: String): List<WeatherCondition>
}
