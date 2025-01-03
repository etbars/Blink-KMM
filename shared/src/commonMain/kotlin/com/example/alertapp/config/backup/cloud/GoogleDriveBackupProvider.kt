package com.example.alertapp.config.backup.cloud

import com.example.alertapp.config.backup.ConfigBackup
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.datetime.Instant
import kotlinx.serialization.json.Json
import co.touchlab.kermit.Logger

/**
 * Google Drive implementation of CloudBackupProvider.
 */
expect class GoogleDriveBackupProvider {
    /**
     * The folder name in Google Drive where backups are stored.
     */
    val BACKUP_FOLDER_NAME: String

    /**
     * The MIME type for backup files.
     */
    val BACKUP_MIME_TYPE: String

    /**
     * JSON serializer for backups.
     */
    val json: Json

    /**
     * Current sync status.
     */
    val _syncStatus: MutableStateFlow<CloudSyncStatus>

    /**
     * Logger instance.
     */
    val logger: Logger

    /**
     * Initialize the provider.
     */
    suspend fun initialize()

    /**
     * Check if authenticated with Google Drive.
     */
    suspend fun isAuthenticated(): Boolean

    /**
     * Authenticate with Google Drive.
     */
    suspend fun authenticate(): Boolean

    /**
     * Upload a backup to Google Drive.
     */
    suspend fun uploadBackup(backup: ConfigBackup): Boolean

    /**
     * Download a backup from Google Drive.
     */
    suspend fun downloadBackup(timestamp: Instant): ConfigBackup?

    /**
     * List all backups in Google Drive.
     */
    suspend fun listBackups(): List<ConfigBackup>

    /**
     * Delete a backup from Google Drive.
     */
    suspend fun deleteBackup(backup: ConfigBackup): Boolean

    /**
     * Delete all backups from Google Drive.
     */
    suspend fun deleteAllBackups(): Boolean

    /**
     * Enable automatic sync with Google Drive.
     */
    suspend fun enableAutoSync(interval: Long)

    /**
     * Disable automatic sync with Google Drive.
     */
    suspend fun disableAutoSync()

    /**
     * Get the sync status.
     */
    fun getSyncStatus(): Flow<CloudSyncStatus>

    /**
     * Force an immediate sync with Google Drive.
     */
    suspend fun forceSync(): Boolean
}
