package com.example.alertapp.config

import platform.Foundation.*
import com.example.alertapp.validation.ValidationResult

/**
 * iOS implementation of ConfigManager.
 */
actual class ConfigManager {
    private val userDefaults = NSUserDefaults.standardUserDefaults

    actual fun getAppConfig(): AlertAppConfig {
        val configString = userDefaults.stringForKey(APP_CONFIG_KEY)
        val config = if (configString != null) {
            json.decodeFromString(AlertAppConfig.serializer(), configString)
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
        val configString = userDefaults.stringForKey(PLATFORM_CONFIG_KEY)
        val config = if (configString != null) {
            json.decodeFromString(PlatformConfig.serializer(), configString)
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
        val configString = userDefaults.stringForKey(WORK_CONFIG_KEY)
        val config = if (configString != null) {
            json.decodeFromString(WorkConfig.serializer(), configString)
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
            val configString = json.encodeToString(AlertAppConfig.serializer(), config)
            userDefaults.setObject(configString, APP_CONFIG_KEY)
            userDefaults.synchronize()
        }
        return validation
    }

    actual fun updatePlatformConfig(config: PlatformConfig): ValidationResult {
        val validation = ConfigValidator.validatePlatformConfig(config)
        if (validation.isValid) {
            val configString = json.encodeToString(PlatformConfig.serializer(), config)
            userDefaults.setObject(configString, PLATFORM_CONFIG_KEY)
            userDefaults.synchronize()
        }
        return validation
    }

    actual fun updateWorkConfig(config: WorkConfig): ValidationResult {
        val validation = ConfigValidator.validateWorkConfig(config)
        if (validation.isValid) {
            val configString = json.encodeToString(WorkConfig.serializer(), config)
            userDefaults.setObject(configString, WORK_CONFIG_KEY)
            userDefaults.synchronize()
        }
        return validation
    }

    companion object {
        private const val APP_CONFIG_KEY = "app_config"
        private const val PLATFORM_CONFIG_KEY = "platform_config"
        private const val WORK_CONFIG_KEY = "work_config"
    }
}
