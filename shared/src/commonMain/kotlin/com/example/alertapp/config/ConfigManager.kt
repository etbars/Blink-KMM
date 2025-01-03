package com.example.alertapp.config

import kotlinx.serialization.json.Json
import com.example.alertapp.validation.ValidationResult
import com.example.alertapp.config.migration.ConfigVersion
import com.example.alertapp.config.migration.ConfigMigrationManager
import com.example.alertapp.config.migration.v1_0_0.InitialMigration

/**
 * Manages configuration for the AlertApp.
 */
expect class ConfigManager {
    /**
     * Get the app configuration.
     * @throws IllegalStateException if the configuration is invalid
     */
    fun getAppConfig(): AlertAppConfig

    /**
     * Get platform-specific configuration.
     * @throws IllegalStateException if the configuration is invalid
     */
    fun getPlatformConfig(): PlatformConfig

    /**
     * Get work-related configuration.
     * @throws IllegalStateException if the configuration is invalid
     */
    fun getWorkConfig(): WorkConfig

    /**
     * Update the app configuration.
     * @return ValidationResult indicating if the update was successful
     */
    fun updateAppConfig(config: AlertAppConfig): ValidationResult

    /**
     * Update platform-specific configuration.
     * @return ValidationResult indicating if the update was successful
     */
    fun updatePlatformConfig(config: PlatformConfig): ValidationResult

    /**
     * Update work-related configuration.
     * @return ValidationResult indicating if the update was successful
     */
    fun updateWorkConfig(config: WorkConfig): ValidationResult

    companion object {
        val json: Json
        val migrationManager: ConfigMigrationManager
    }
}
