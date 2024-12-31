package com.example.alertapp.android.backup

import android.app.backup.*
import android.os.ParcelFileDescriptor
import com.example.alertapp.config.ConfigManager
import com.example.alertapp.config.backup.ConfigBackupManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.runBlocking
import java.io.File
import javax.inject.Inject

@AndroidEntryPoint
class AlertBackupAgent : BackupAgentHelper() {
    @Inject lateinit var configManager: ConfigManager
    @Inject lateinit var backupManager: ConfigBackupManager

    companion object {
        private const val ALERTS_BACKUP_KEY = "alerts_backup"
        private const val CONFIG_BACKUP_KEY = "config_backup"
        private const val SHARED_PREFS_BACKUP_KEY = "shared_prefs_backup"
    }

    override fun onCreate() {
        super.onCreate()

        // Back up the alerts database
        val alertsHelper = FileBackupHelper(this, "../databases/alerts.db")
        addHelper(ALERTS_BACKUP_KEY, alertsHelper)

        // Back up the configuration files
        val configHelper = FileBackupHelper(this, "../files/config")
        addHelper(CONFIG_BACKUP_KEY, configHelper)

        // Back up shared preferences
        val sharedPrefsHelper = SharedPreferencesBackupHelper(this, "app_settings")
        addHelper(SHARED_PREFS_BACKUP_KEY, sharedPrefsHelper)
    }

    override fun onBackup(
        oldState: ParcelFileDescriptor?,
        data: BackupDataOutput?,
        newState: ParcelFileDescriptor?
    ) {
        synchronized(AlertBackupLock) {
            super.onBackup(oldState, data, newState)
        }
    }

    override fun onRestore(
        data: BackupDataInput?,
        appVersionCode: Int,
        newState: ParcelFileDescriptor?
    ) {
        synchronized(AlertBackupLock) {
            super.onRestore(data, appVersionCode, newState)
        }
    }

    override fun onRestoreFinished() {
        super.onRestoreFinished()
        
        // Reinitialize after restore
        runBlocking {
            try {
                configManager.initialize()
                backupManager.initialize()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}

object AlertBackupLock
