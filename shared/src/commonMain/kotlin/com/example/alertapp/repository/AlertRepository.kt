package com.example.alertapp.repository

import com.example.alertapp.models.Alert
import kotlinx.coroutines.flow.Flow

interface AlertRepository {
    suspend fun createAlert(alert: Alert): String
    suspend fun getAlert(id: String): Alert?
    suspend fun updateAlert(alert: Alert): Boolean
    suspend fun deleteAlert(id: String): Boolean
    suspend fun getAlerts(): Flow<List<Alert>>
    suspend fun getEnabledAlerts(): Flow<List<Alert>>
    suspend fun updateLastTriggered(id: String)
}

// Platform-specific implementations will be in androidMain and iosMain
