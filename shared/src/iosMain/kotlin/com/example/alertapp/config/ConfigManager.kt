package com.example.alertapp.config

import com.example.alertapp.validation.ValidationResult
import kotlinx.serialization.json.Json
import com.example.alertapp.config.migration.ConfigMigrationManager
import com.example.alertapp.config.migration.v1_0_0.InitialMigration
import platform.Foundation.*

/**
 * iOS implementation of ConfigManager.
 */
actual class ConfigManager {
    private val userDefaults = NSUserDefaults.standardUserDefaults

    actual fun getAppConfig(): AlertAppConfig {
        val jsonString = userDefaults.stringForKey(APP_CONFIG_KEY)
            ?: throw IllegalStateException("App configuration not found")
        return json.decodeFromString(AlertAppConfig.serializer(), jsonString)
    }

    actual fun getPlatformConfig(): PlatformConfig {
        val jsonString = userDefaults.stringForKey(PLATFORM_CONFIG_KEY)
            ?: throw IllegalStateException("Platform configuration not found")
        return json.decodeFromString(PlatformConfig.serializer(), jsonString)
    }

    actual fun getWorkConfig(): WorkConfig {
        val jsonString = userDefaults.stringForKey(WORK_CONFIG_KEY)
            ?: throw IllegalStateException("Work configuration not found")
        return json.decodeFromString(WorkConfig.serializer(), jsonString)
    }

    actual fun updateAppConfig(config: AlertAppConfig): ValidationResult {
        val jsonString = json.encodeToString(AlertAppConfig.serializer(), config)
        userDefaults.setObject(jsonString, APP_CONFIG_KEY)
        return ValidationResult.Success
    }

    actual fun updatePlatformConfig(config: PlatformConfig): ValidationResult {
        val jsonString = json.encodeToString(PlatformConfig.serializer(), config)
        userDefaults.setObject(jsonString, PLATFORM_CONFIG_KEY)
        return ValidationResult.Success
    }

    actual fun updateWorkConfig(config: WorkConfig): ValidationResult {
        val jsonString = json.encodeToString(WorkConfig.serializer(), config)
        userDefaults.setObject(jsonString, WORK_CONFIG_KEY)
        return ValidationResult.Success
    }

    companion object {
        private const val APP_CONFIG_KEY = "app_config"
        private const val PLATFORM_CONFIG_KEY = "platform_config"
        private const val WORK_CONFIG_KEY = "work_config"

        actual val json: Json = Json {
            ignoreUnknownKeys = true
            isLenient = true
            prettyPrint = true
        }

        actual val migrationManager: ConfigMigrationManager = ConfigMigrationManager(
            migrations = listOf(
                InitialMigration()
            )
        )
    }
}
