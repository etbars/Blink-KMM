package com.example.alertapp.android.processors

import com.example.alertapp.android.data.AlertRepository
import com.example.alertapp.android.notifications.AlertNotificationManager
import com.example.alertapp.models.Alert
import com.example.alertapp.models.AlertType
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AndroidAlertProcessor @Inject constructor(
    private val weatherAlertProcessor: WeatherAlertProcessor,
    private val priceAlertProcessor: PriceAlertProcessor,
    private val contentAlertProcessor: ContentAlertProcessor,
    private val notificationManager: AlertNotificationManager,
    private val alertRepository: AlertRepository
) {
    suspend fun processAlert(alertId: String): Boolean {
        val alert = alertRepository.getAlertById(alertId) ?: return false
        return processAlert(alert)
    }

    suspend fun processAlert(alert: Alert): Boolean {
        if (!alert.isActive) return false

        val triggered = when (alert.type) {
            AlertType.WEATHER -> weatherAlertProcessor.processAlert(alert)
            AlertType.PRICE -> priceAlertProcessor.processAlert(alert)
            AlertType.CONTENT -> contentAlertProcessor.processAlert(alert)
            else -> false
        }

        if (triggered) {
            // Update last triggered time
            alertRepository.updateLastTriggered(alert.id)
            
            // Show notification for triggered alert
            formatAlertMessage(alert)?.let { message ->
                notificationManager.showAlertNotification(alert, message)
            }
        }

        return triggered
    }

    suspend fun formatAlertMessage(alert: Alert): String? {
        if (!alert.isActive) return null

        return when (alert.type) {
            AlertType.WEATHER -> weatherAlertProcessor.formatWeatherMessage(alert)
            AlertType.PRICE -> priceAlertProcessor.formatPriceMessage(alert)
            AlertType.CONTENT -> contentAlertProcessor.formatContentMessage(alert)
            else -> null
        }
    }
}
