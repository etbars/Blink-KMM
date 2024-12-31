package com.example.alertapp.services.processors

import com.example.alertapp.models.Alert
import com.example.alertapp.models.WeatherConditionRule
import com.example.alertapp.models.WeatherData
import com.example.alertapp.network.services.WeatherApiService
import com.example.alertapp.services.base.NotificationHandler
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AndroidWeatherAlertProcessor @Inject constructor(
    private val notificationHandler: NotificationHandler,
    private val weatherApiService: WeatherApiService,
    private val locationProvider: LocationProvider
) : WeatherAlertProcessor(notificationHandler, weatherApiService, locationProvider) {

    override suspend fun getWeatherData(location: Location): WeatherData? {
        return try {
            weatherApiService.getWeatherData(location.latitude, location.longitude)
        } catch (e: Exception) {
            logError(Alert(), "Failed to fetch weather data", e)
            null
        }
    }

    override fun getLocationFromAlert(alert: Alert): Location? {
        val locationStr = alert.trigger.toString()
        return locationProvider.parseLocation(locationStr)
    }

    override fun getConditionsFromAlert(alert: Alert): List<WeatherConditionRule> {
        return try {
            when (val trigger = alert.trigger) {
                is AlertTrigger.CustomTrigger -> {
                    trigger.parameters["conditions"]?.let { conditionsStr ->
                        locationProvider.parseWeatherConditions(conditionsStr)
                    } ?: emptyList()
                }
                else -> emptyList()
            }
        } catch (e: Exception) {
            logError(alert, "Failed to parse weather conditions", e)
            emptyList()
        }
    }

    override fun logWarning(alert: Alert, message: String) {
        Timber.w("Alert ${alert.id}: $message")
    }

    override fun logError(alert: Alert, message: String, error: Throwable?) {
        if (error != null) {
            Timber.e(error, "Alert ${alert.id}: $message")
        } else {
            Timber.e("Alert ${alert.id}: $message")
        }
    }

    override fun logInfo(alert: Alert, message: String) {
        Timber.i("Alert ${alert.id}: $message")
    }
}
