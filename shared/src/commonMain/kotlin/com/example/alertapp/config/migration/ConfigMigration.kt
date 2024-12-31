package com.example.alertapp.config.migration

import com.example.alertapp.config.*
import kotlinx.serialization.json.*

/**
 * Base class for configuration migrations.
 */
abstract class ConfigMigration(
    val fromVersion: ConfigVersion,
    val toVersion: ConfigVersion
) {
    init {
        require(fromVersion < toVersion) { 
            "Migration must be from older to newer version" 
        }
    }

    /**
     * Migrate app configuration from one version to another.
     * @throws MigrationException if migration fails
     */
    abstract fun migrateAppConfig(config: JsonObject): JsonObject

    /**
     * Roll back app configuration migration.
     * Default implementation returns the original config.
     * Override this method to provide custom rollback logic.
     */
    open fun rollbackAppConfig(config: JsonObject, error: Throwable): JsonObject = config

    /**
     * Migrate platform configuration from one version to another.
     * @throws MigrationException if migration fails
     */
    abstract fun migratePlatformConfig(config: JsonObject): JsonObject

    /**
     * Roll back platform configuration migration.
     * Default implementation returns the original config.
     * Override this method to provide custom rollback logic.
     */
    open fun rollbackPlatformConfig(config: JsonObject, error: Throwable): JsonObject = config

    /**
     * Migrate work configuration from one version to another.
     * @throws MigrationException if migration fails
     */
    abstract fun migrateWorkConfig(config: JsonObject): JsonObject

    /**
     * Roll back work configuration migration.
     * Default implementation returns the original config.
     * Override this method to provide custom rollback logic.
     */
    open fun rollbackWorkConfig(config: JsonObject, error: Throwable): JsonObject = config

    /**
     * Helper function to safely get a JsonElement from a JsonObject.
     */
    protected fun JsonObject.getOrNull(key: String): JsonElement? = get(key)

    /**
     * Helper function to safely get a String from a JsonObject.
     */
    protected fun JsonObject.getString(key: String): String? =
        getOrNull(key)?.jsonPrimitive?.contentOrNull

    /**
     * Helper function to safely get a Number from a JsonObject.
     */
    protected fun JsonObject.getNumber(key: String): Number? =
        getOrNull(key)?.jsonPrimitive?.doubleOrNull

    /**
     * Helper function to safely get a Boolean from a JsonObject.
     */
    protected fun JsonObject.getBoolean(key: String): Boolean? =
        getOrNull(key)?.jsonPrimitive?.booleanOrNull
}
