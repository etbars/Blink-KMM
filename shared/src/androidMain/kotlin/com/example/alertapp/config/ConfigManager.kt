package com.example.alertapp.config

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import com.example.alertapp.validation.ValidationResult
import kotlinx.serialization.json.Json
import com.example.alertapp.config.migration.ConfigMigrationManager
import com.example.alertapp.config.migration.v1_0_0.InitialMigration
import org.koin.core.component.KoinComponent
import javax.inject.Singleton

@Singleton
actual class ConfigManager(
    private val context: Context
) : KoinComponent {
    private val sharedPreferences: SharedPreferences by lazy {
        context.getSharedPreferences("config_prefs", Context.MODE_PRIVATE)
    }

    actual fun getAppConfig(): AlertAppConfig {
        val configJson = sharedPreferences.getString(APP_CONFIG_KEY, null)
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
        val configJson = sharedPreferences.getString(PLATFORM_CONFIG_KEY, null)
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
        val configJson = sharedPreferences.getString(WORK_CONFIG_KEY, null)
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
        if (!validation.isValid) {
            return validation
        }

        val configJson = json.encodeToString(AlertAppConfig.serializer(), config)
        sharedPreferences.edit().putString(APP_CONFIG_KEY, configJson).apply()
        return validation
    }

    actual fun updatePlatformConfig(config: PlatformConfig): ValidationResult {
        val validation = ConfigValidator.validatePlatformConfig(config)
        if (!validation.isValid) {
            return validation
        }

        val configJson = json.encodeToString(PlatformConfig.serializer(), config)
        sharedPreferences.edit().putString(PLATFORM_CONFIG_KEY, configJson).apply()
        return validation
    }

    actual fun updateWorkConfig(config: WorkConfig): ValidationResult {
        val validation = ConfigValidator.validateWorkConfig(config)
        if (!validation.isValid) {
            return validation
        }

        val configJson = json.encodeToString(WorkConfig.serializer(), config)
        sharedPreferences.edit().putString(WORK_CONFIG_KEY, configJson).apply()
        return validation
    }

    actual companion object {
        actual val json: Json = Json {
            prettyPrint = true
            ignoreUnknownKeys = true
            isLenient = true
        }

        actual val migrationManager: ConfigMigrationManager = ConfigMigrationManager()

        private const val APP_CONFIG_KEY = "app_config"
        private const val PLATFORM_CONFIG_KEY = "platform_config"
        private const val WORK_CONFIG_KEY = "work_config"
    }
}
