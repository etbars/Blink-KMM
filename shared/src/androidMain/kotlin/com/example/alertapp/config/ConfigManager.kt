package com.example.alertapp.config

import android.content.Context
import android.content.SharedPreferences
import com.example.alertapp.validation.ValidationResult
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Android implementation of ConfigManager.
 */
@Singleton
actual class ConfigManager @Inject constructor(
    private val context: Context
) {
    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences("config_prefs", Context.MODE_PRIVATE)
    }

    actual fun getAppConfig(): AlertAppConfig {
        val configJson = prefs.getString(APP_CONFIG_KEY, null)
        val config = if (configJson != null) {
            json.decodeFromString(AlertAppConfig.serializer(), configJson)
        } else {
            AlertAppConfig()
        }
        
        val validation = ConfigValidator.validateAppConfig(config)
        if (!validation.isValid) {
            throw IllegalStateException("Invalid app configuration: ${validation.getFormattedMessages()}")
        }
        
        return config
    }

    actual fun getPlatformConfig(): PlatformConfig {
        val configJson = prefs.getString(PLATFORM_CONFIG_KEY, null)
        val config = if (configJson != null) {
            json.decodeFromString(PlatformConfig.serializer(), configJson)
        } else {
            PlatformConfig()
        }
        
        val validation = ConfigValidator.validatePlatformConfig(config)
        if (!validation.isValid) {
            throw IllegalStateException("Invalid platform configuration: ${validation.getFormattedMessages()}")
        }
        
        return config
    }

    actual fun getWorkConfig(): WorkConfig {
        val configJson = prefs.getString(WORK_CONFIG_KEY, null)
        val config = if (configJson != null) {
            json.decodeFromString(WorkConfig.serializer(), configJson)
        } else {
            WorkConfig()
        }
        
        val validation = ConfigValidator.validateWorkConfig(config)
        if (!validation.isValid) {
            throw IllegalStateException("Invalid work configuration: ${validation.getFormattedMessages()}")
        }
        
        return config
    }

    actual fun updateAppConfig(config: AlertAppConfig): ValidationResult {
        val validation = ConfigValidator.validateAppConfig(config)
        if (validation.isValid) {
            val configJson = json.encodeToString(AlertAppConfig.serializer(), config)
            prefs.edit().putString(APP_CONFIG_KEY, configJson).apply()
        }
        return validation
    }

    actual fun updatePlatformConfig(config: PlatformConfig): ValidationResult {
        val validation = ConfigValidator.validatePlatformConfig(config)
        if (validation.isValid) {
            val configJson = json.encodeToString(PlatformConfig.serializer(), config)
            prefs.edit().putString(PLATFORM_CONFIG_KEY, configJson).apply()
        }
        return validation
    }

    actual fun updateWorkConfig(config: WorkConfig): ValidationResult {
        val validation = ConfigValidator.validateWorkConfig(config)
        if (validation.isValid) {
            val configJson = json.encodeToString(WorkConfig.serializer(), config)
            prefs.edit().putString(WORK_CONFIG_KEY, configJson).apply()
        }
        return validation
    }

    companion object {
        private const val APP_CONFIG_KEY = "app_config"
        private const val PLATFORM_CONFIG_KEY = "platform_config"
        private const val WORK_CONFIG_KEY = "work_config"
    }
}
