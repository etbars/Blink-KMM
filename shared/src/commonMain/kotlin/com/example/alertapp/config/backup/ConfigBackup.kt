package com.example.alertapp.config.backup

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*
import kotlinx.datetime.Instant
import com.example.alertapp.config.migration.ConfigVersion

/**
 * Represents a backup of app configurations.
 */
@Serializable
data class ConfigBackup(
    val timestamp: Instant,
    val version: ConfigVersion,
    val appConfig: JsonObject?,
    val platformConfig: JsonObject?,
    val workConfig: JsonObject?,
    val metadata: Map<String, String> = emptyMap()
) {
    companion object {
        const val BACKUP_FILE_EXTENSION = ".backup.json"
        const val MAX_BACKUPS = 5
    }
}
