package com.example.alertapp.android.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.alertapp.android.work.AlertWorkManager
import com.example.alertapp.config.ConfigManager
import com.example.alertapp.config.backup.ConfigBackupManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val configManager: ConfigManager,
    private val backupManager: ConfigBackupManager,
    private val workManager: AlertWorkManager
) : ViewModel() {

    private val _settings = MutableStateFlow(AppSettings())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    init {
        viewModelScope.launch {
            loadSettings()
        }
    }

    private suspend fun loadSettings() {
        val config = configManager.getConfig()
        _settings.value = AppSettings(
            notificationsEnabled = config.notificationsEnabled ?: true,
            vibrateEnabled = config.vibrateEnabled ?: true,
            backgroundSyncEnabled = config.backgroundSyncEnabled ?: true,
            syncInterval = config.syncInterval ?: "30 minutes",
            autoBackupEnabled = config.autoBackupEnabled ?: true,
            wifiOnlyEnabled = config.wifiOnlyEnabled ?: false
        )
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            _settings.update { it.copy(notificationsEnabled = enabled) }
            configManager.updateConfig { it.copy(notificationsEnabled = enabled) }
        }
    }

    fun setVibrateEnabled(enabled: Boolean) {
        viewModelScope.launch {
            _settings.update { it.copy(vibrateEnabled = enabled) }
            configManager.updateConfig { it.copy(vibrateEnabled = enabled) }
        }
    }

    fun setBackgroundSyncEnabled(enabled: Boolean) {
        viewModelScope.launch {
            _settings.update { it.copy(backgroundSyncEnabled = enabled) }
            configManager.updateConfig { it.copy(backgroundSyncEnabled = enabled) }
            
            if (enabled) {
                workManager.schedulePeriodicSync(settings.value.syncInterval)
            } else {
                workManager.cancelPeriodicSync()
            }
        }
    }

    fun setSyncInterval(interval: String) {
        viewModelScope.launch {
            _settings.update { it.copy(syncInterval = interval) }
            configManager.updateConfig { it.copy(syncInterval = interval) }
            
            if (settings.value.backgroundSyncEnabled) {
                workManager.schedulePeriodicSync(interval)
            }
        }
    }

    fun setAutoBackupEnabled(enabled: Boolean) {
        viewModelScope.launch {
            _settings.update { it.copy(autoBackupEnabled = enabled) }
            configManager.updateConfig { it.copy(autoBackupEnabled = enabled) }
            
            if (enabled) {
                workManager.schedulePeriodicBackup()
            } else {
                workManager.cancelPeriodicBackup()
            }
        }
    }

    fun setWifiOnlyEnabled(enabled: Boolean) {
        viewModelScope.launch {
            _settings.update { it.copy(wifiOnlyEnabled = enabled) }
            configManager.updateConfig { it.copy(wifiOnlyEnabled = enabled) }
            
            // Update work constraints
            if (settings.value.backgroundSyncEnabled) {
                workManager.schedulePeriodicSync(settings.value.syncInterval)
            }
        }
    }

    fun createBackup() {
        viewModelScope.launch {
            try {
                backupManager.createBackup()
            } catch (e: Exception) {
                // Handle backup error
                e.printStackTrace()
            }
        }
    }

    fun restoreBackup() {
        viewModelScope.launch {
            try {
                backupManager.restoreLatestBackup()
                // Reload settings after restore
                loadSettings()
            } catch (e: Exception) {
                // Handle restore error
                e.printStackTrace()
            }
        }
    }
}

data class AppSettings(
    val notificationsEnabled: Boolean = true,
    val vibrateEnabled: Boolean = true,
    val backgroundSyncEnabled: Boolean = true,
    val syncInterval: String = "30 minutes",
    val autoBackupEnabled: Boolean = true,
    val wifiOnlyEnabled: Boolean = false
)
