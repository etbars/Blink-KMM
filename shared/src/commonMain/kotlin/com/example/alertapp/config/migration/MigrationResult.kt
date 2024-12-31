package com.example.alertapp.config.migration

import kotlinx.serialization.json.JsonObject

/**
 * Represents the result of a configuration migration.
 */
sealed class MigrationResult {
    /**
     * Migration completed successfully.
     */
    data class Success(
        val config: JsonObject,
        val fromVersion: ConfigVersion,
        val toVersion: ConfigVersion
    ) : MigrationResult()

    /**
     * Migration failed and was rolled back.
     */
    data class Failure(
        val error: Throwable,
        val fromVersion: ConfigVersion,
        val toVersion: ConfigVersion,
        val rollbackVersion: ConfigVersion?,
        val originalConfig: JsonObject
    ) : MigrationResult() {
        val message: String = buildString {
            append("Migration failed from version $fromVersion to $toVersion: ${error.message}")
            if (rollbackVersion != null) {
                append(". Rolled back to version $rollbackVersion")
            }
        }
    }
}
