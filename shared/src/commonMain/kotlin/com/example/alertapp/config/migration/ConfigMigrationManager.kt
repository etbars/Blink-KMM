package com.example.alertapp.config.migration

import com.example.alertapp.config.*
import kotlinx.serialization.json.*

/**
 * Manages configuration migrations between versions.
 */
class ConfigMigrationManager {
    private val migrations = mutableListOf<ConfigMigration>()

    /**
     * Register a migration to be used when upgrading configurations.
     */
    fun registerMigration(migration: ConfigMigration) {
        migrations.add(migration)
        migrations.sortBy { it.fromVersion }
    }

    /**
     * Migrate app configuration from one version to another.
     */
    fun migrateAppConfig(
        config: AlertAppConfig,
        fromVersion: ConfigVersion
    ): AlertAppConfig {
        if (fromVersion >= ConfigVersion.CURRENT) return config

        val json = ConfigManager.json
        val originalJson = json.encodeToJsonElement(AlertAppConfig.serializer(), config).jsonObject
        
        return when (val result = migrateJson(originalJson, fromVersion) { migration, jsonConfig ->
            migration.migrateAppConfig(jsonConfig)
        }) {
            is MigrationResult.Success -> {
                json.decodeFromJsonElement(AlertAppConfig.serializer(), result.config)
            }
            is MigrationResult.Failure -> {
                throw MigrationException(
                    result.message,
                    result.fromVersion,
                    result.toVersion,
                    result.error
                )
            }
        }
    }

    /**
     * Migrate platform configuration from one version to another.
     */
    fun migratePlatformConfig(
        config: PlatformConfig,
        fromVersion: ConfigVersion
    ): PlatformConfig {
        if (fromVersion >= ConfigVersion.CURRENT) return config

        val json = ConfigManager.json
        val originalJson = json.encodeToJsonElement(PlatformConfig.serializer(), config).jsonObject
        
        return when (val result = migrateJson(originalJson, fromVersion) { migration, jsonConfig ->
            migration.migratePlatformConfig(jsonConfig)
        }) {
            is MigrationResult.Success -> {
                json.decodeFromJsonElement(PlatformConfig.serializer(), result.config)
            }
            is MigrationResult.Failure -> {
                throw MigrationException(
                    result.message,
                    result.fromVersion,
                    result.toVersion,
                    result.error
                )
            }
        }
    }

    /**
     * Migrate work configuration from one version to another.
     */
    fun migrateWorkConfig(
        config: WorkConfig,
        fromVersion: ConfigVersion
    ): WorkConfig {
        if (fromVersion >= ConfigVersion.CURRENT) return config

        val json = ConfigManager.json
        val originalJson = json.encodeToJsonElement(WorkConfig.serializer(), config).jsonObject
        
        return when (val result = migrateJson(originalJson, fromVersion) { migration, jsonConfig ->
            migration.migrateWorkConfig(jsonConfig)
        }) {
            is MigrationResult.Success -> {
                json.decodeFromJsonElement(WorkConfig.serializer(), result.config)
            }
            is MigrationResult.Failure -> {
                throw MigrationException(
                    result.message,
                    result.fromVersion,
                    result.toVersion,
                    result.error
                )
            }
        }
    }

    /**
     * Get the path of migrations needed to go from one version to another.
     */
    private fun getMigrationPath(
        fromVersion: ConfigVersion,
        toVersion: ConfigVersion
    ): List<ConfigMigration> {
        if (fromVersion >= toVersion) return emptyList()

        val path = mutableListOf<ConfigMigration>()
        var currentVersion = fromVersion

        while (currentVersion < toVersion) {
            val nextMigration = migrations.firstOrNull { 
                it.fromVersion == currentVersion 
            } ?: throw IllegalStateException(
                "No migration path found from $currentVersion to $toVersion"
            )

            path.add(nextMigration)
            currentVersion = nextMigration.toVersion
        }

        return path
    }

    /**
     * Migrate a JSON configuration through a series of migrations with rollback support.
     */
    private fun migrateJson(
        config: JsonObject,
        fromVersion: ConfigVersion,
        migrationFunction: (ConfigMigration, JsonObject) -> JsonObject
    ): MigrationResult {
        if (fromVersion >= ConfigVersion.CURRENT) {
            return MigrationResult.Success(config, fromVersion, fromVersion)
        }

        val migrationPath = getMigrationPath(fromVersion, ConfigVersion.CURRENT)
        var currentConfig = config
        var currentVersion = fromVersion
        val appliedMigrations = mutableListOf<ConfigMigration>()

        try {
            migrationPath.forEach { migration ->
                currentConfig = migrationFunction(migration, currentConfig)
                appliedMigrations.add(migration)
                currentVersion = migration.toVersion
            }

            return MigrationResult.Success(currentConfig, fromVersion, ConfigVersion.CURRENT)
        } catch (e: Exception) {
            // Roll back migrations in reverse order
            var rolledBackConfig = currentConfig
            var rolledBackVersion = currentVersion

            appliedMigrations.asReversed().forEach { migration ->
                try {
                    when (migrationFunction) {
                        migration::migrateAppConfig -> {
                            rolledBackConfig = migration.rollbackAppConfig(rolledBackConfig, e)
                        }
                        migration::migratePlatformConfig -> {
                            rolledBackConfig = migration.rollbackPlatformConfig(rolledBackConfig, e)
                        }
                        migration::migrateWorkConfig -> {
                            rolledBackConfig = migration.rollbackWorkConfig(rolledBackConfig, e)
                        }
                    }
                    rolledBackVersion = migration.fromVersion
                } catch (rollbackError: Exception) {
                    // If rollback fails, continue with the next rollback
                    // but keep the original error as the primary cause
                }
            }

            return MigrationResult.Failure(
                error = e,
                fromVersion = fromVersion,
                toVersion = ConfigVersion.CURRENT,
                rollbackVersion = rolledBackVersion,
                originalConfig = config
            )
        }
    }
}
