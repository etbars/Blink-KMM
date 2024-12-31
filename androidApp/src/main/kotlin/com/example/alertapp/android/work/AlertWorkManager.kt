package com.example.alertapp.android.work

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.work.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AlertWorkManager @Inject constructor(
    private val context: Context
) {
    companion object {
        private const val SYNC_WORK_NAME = "alert_sync_work"
        private const val BACKUP_WORK_NAME = "alert_backup_work"
        private const val DEFAULT_BACKUP_INTERVAL_HOURS = 24L
    }

    private val workManager = WorkManager.getInstance(context)

    fun schedulePeriodicSync(interval: String) {
        val intervalMinutes = when (interval) {
            "15 minutes" -> 15L
            "30 minutes" -> 30L
            "1 hour" -> 60L
            "2 hours" -> 120L
            else -> 30L
        }

        val constraints = createWorkConstraints()

        val syncRequest = PeriodicWorkRequestBuilder<AlertSyncWorker>(
            intervalMinutes,
            TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .build()

        workManager.enqueueUniquePeriodicWork(
            SYNC_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            syncRequest
        )
    }

    fun schedulePeriodicBackup() {
        val constraints = createWorkConstraints()

        val backupRequest = PeriodicWorkRequestBuilder<AlertBackupWorker>(
            DEFAULT_BACKUP_INTERVAL_HOURS,
            TimeUnit.HOURS
        )
            .setConstraints(constraints)
            .build()

        workManager.enqueueUniquePeriodicWork(
            BACKUP_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            backupRequest
        )
    }

    fun cancelPeriodicSync() {
        workManager.cancelUniqueWork(SYNC_WORK_NAME)
    }

    fun cancelPeriodicBackup() {
        workManager.cancelUniqueWork(BACKUP_WORK_NAME)
    }

    private fun createWorkConstraints(): Constraints {
        return Constraints.Builder()
            .setRequiredNetworkType(
                if (isWifiOnlyEnabled()) NetworkType.UNMETERED
                else NetworkType.CONNECTED
            )
            .setRequiresBatteryNotLow(true)
            .build()
    }

    private fun isWifiOnlyEnabled(): Boolean {
        return try {
            val config = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
            config.getBoolean("wifi_only_enabled", false)
        } catch (e: Exception) {
            false
        }
    }
}
