package com.example.alertapp.android

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.example.alertapp.config.ConfigManager
import com.example.alertapp.config.backup.ConfigBackupManager
import com.example.alertapp.services.AlertProcessor
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class AlertApplication : Application() {
    @Inject lateinit var configManager: ConfigManager
    @Inject lateinit var backupManager: ConfigBackupManager
    @Inject lateinit var alertProcessor: AlertProcessor

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    companion object {
        const val ALERT_CHANNEL_ID = "alert_channel"
        const val ALERT_CHANNEL_NAME = "Alerts"
        const val ALERT_CHANNEL_DESCRIPTION = "Notifications for alerts"
    }

    override fun onCreate() {
        super.onCreate()
        
        // Initialize notification channels
        createNotificationChannel()
        
        // Initialize configurations
        applicationScope.launch {
            try {
                configManager.initialize()
                backupManager.initialize()
            } catch (e: Exception) {
                // Handle initialization error
                e.printStackTrace()
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                ALERT_CHANNEL_ID,
                ALERT_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = ALERT_CHANNEL_DESCRIPTION
                enableVibration(true)
                enableLights(true)
            }

            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) 
                as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}
