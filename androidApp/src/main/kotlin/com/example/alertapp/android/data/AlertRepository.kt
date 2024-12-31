package com.example.alertapp.android.data

import com.example.alertapp.models.Alert
import com.example.alertapp.models.AlertType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Clock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AlertRepository @Inject constructor(
    private val alertDao: AlertDao
) {
    fun getAllAlerts(): Flow<List<Alert>> {
        return alertDao.getAllAlerts().map { entities ->
            entities.map { it.toAlert() }
        }
    }

    fun getAlertsByType(type: AlertType): Flow<List<Alert>> {
        return alertDao.getAlertsByType(type).map { entities ->
            entities.map { it.toAlert() }
        }
    }

    fun getActiveAlerts(): Flow<List<Alert>> {
        return alertDao.getActiveAlerts().map { entities ->
            entities.map { it.toAlert() }
        }
    }

    suspend fun getAlertById(id: String): Alert? {
        return alertDao.getAlertById(id)?.toAlert()
    }

    suspend fun insertAlert(alert: Alert) {
        alertDao.insertAlert(AlertEntity.fromAlert(alert))
    }

    suspend fun updateAlert(alert: Alert) {
        alertDao.updateAlert(AlertEntity.fromAlert(alert))
    }

    suspend fun deleteAlert(alert: Alert) {
        alertDao.deleteAlert(AlertEntity.fromAlert(alert))
    }

    suspend fun deleteAlertById(id: String) {
        alertDao.deleteAlertById(id)
    }

    suspend fun updateLastTriggered(id: String) {
        alertDao.updateLastTriggered(id, Clock.System.now().toEpochMilliseconds())
    }

    suspend fun setAlertActive(id: String, isActive: Boolean) {
        alertDao.updateAlertActive(id, isActive)
    }

    fun getAlertsByUser(userId: String): Flow<List<Alert>> {
        return alertDao.getAlertsByUser(userId).map { entities ->
            entities.map { it.toAlert() }
        }
    }

    suspend fun getAlertsForInterval(maxInterval: Long): List<Alert> {
        return alertDao.getAlertsForInterval(maxInterval).map { it.toAlert() }
    }
}
