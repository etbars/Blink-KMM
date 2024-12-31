package com.example.alertapp.repository

import com.example.alertapp.models.Alert

interface AlertRepository {
    suspend fun createAlert(alert: Alert): String
    suspend fun getAlert(id: String): Alert?
    suspend fun updateAlert(alert: Alert): Boolean
    suspend fun deleteAlert(id: String): Boolean
    suspend fun getAlerts(userId: String): List<Alert>
}

// Platform-specific implementations will be in androidMain and iosMain
