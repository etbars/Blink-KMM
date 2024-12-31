package com.example.alertapp.config.backup

import com.example.alertapp.config.*
import com.example.alertapp.config.migration.ConfigVersion
import kotlinx.datetime.Clock
import kotlinx.serialization.json.*
import okio.FileSystem
import okio.Path.Companion.toPath

/**
 * Manages configuration backups.
 */
expect class ConfigBackupManager {
    /**
     * Create a backup of the current configuration.
     * @param metadata Additional metadata to store with the backup
     * @return The created backup
     */
    suspend fun createBackup(metadata: Map<String, String> = emptyMap()): ConfigBackup

    /**
     * Restore configuration from a backup.
     * @param backup The backup to restore from
     * @return True if restore was successful
     */
    suspend fun restoreBackup(backup: ConfigBackup): Boolean

    /**
     * List all available backups.
     * @return List of backups sorted by timestamp (newest first)
     */
    suspend fun listBackups(): List<ConfigBackup>

    /**
     * Delete a specific backup.
     * @param backup The backup to delete
     * @return True if deletion was successful
     */
    suspend fun deleteBackup(backup: ConfigBackup): Boolean

    /**
     * Delete all backups.
     * @return Number of backups deleted
     */
    suspend fun deleteAllBackups(): Int

    companion object {
        /**
         * Create a backup from the current configuration state.
         */
        fun createBackupFromConfig(
            appConfig: AlertAppConfig?,
            platformConfig: PlatformConfig?,
            workConfig: WorkConfig?,
            metadata: Map<String, String> = emptyMap()
        ): ConfigBackup {
            val json = ConfigManager.json
            
            return ConfigBackup(
                timestamp = Clock.System.now(),
                version = ConfigVersion.CURRENT,
                appConfig = appConfig?.let { 
                    json.encodeToJsonElement(AlertAppConfig.serializer(), it).jsonObject 
                },
                platformConfig = platformConfig?.let { 
                    json.encodeToJsonElement(PlatformConfig.serializer(), it).jsonObject 
                },
                workConfig = workConfig?.let { 
                    json.encodeToJsonElement(WorkConfig.serializer(), it).jsonObject 
                },
                metadata = metadata
            )
        }

        /**
         * Generate a backup filename based on timestamp.
         */
        fun generateBackupFilename(timestamp: Instant): String {
            return "config_${timestamp.toString().replace(":", "-")}" + 
                    ConfigBackup.BACKUP_FILE_EXTENSION
        }

        /**
         * Clean up old backups, keeping only the most recent ones.
         */
        suspend fun cleanupOldBackups(
            backups: List<ConfigBackup>,
            fileSystem: FileSystem,
            backupDir: String
        ): Int {
            if (backups.size <= ConfigBackup.MAX_BACKUPS) return 0

            val backupsToDelete = backups
                .sortedByDescending { it.timestamp }
                .drop(ConfigBackup.MAX_BACKUPS)

            backupsToDelete.forEach { backup ->
                val filename = generateBackupFilename(backup.timestamp)
                val path = "$backupDir/$filename".toPath()
                try {
                    fileSystem.delete(path)
                } catch (e: Exception) {
                    // Log error but continue with other deletions
                }
            }

            return backupsToDelete.size
        }
    }
}
