package com.example.alertapp.config.backup.cloud

import com.example.alertapp.config.backup.ConfigBackup
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.Instant

/**
 * Interface for cloud backup providers.
 * Implementations should handle authentication and data sync with specific cloud services.
 */
interface CloudBackupProvider {
    /**
     * Check if the provider is authenticated and ready to use.
     */
    suspend fun isAuthenticated(): Boolean

    /**
     * Authenticate with the cloud service.
     * @return true if authentication was successful
     */
    suspend fun authenticate(): Boolean

    /**
     * Upload a backup to the cloud.
     * @param backup The backup to upload
     * @return true if upload was successful
     */
    suspend fun uploadBackup(backup: ConfigBackup): Boolean

    /**
     * Download a backup from the cloud.
     * @param timestamp The timestamp of the backup to download
     * @return The downloaded backup or null if not found
     */
    suspend fun downloadBackup(timestamp: Instant): ConfigBackup?

    /**
     * List all available cloud backups.
     * @return List of backups sorted by timestamp (newest first)
     */
    suspend fun listBackups(): List<ConfigBackup>

    /**
     * Delete a backup from the cloud.
     * @param backup The backup to delete
     * @return true if deletion was successful
     */
    suspend fun deleteBackup(backup: ConfigBackup): Boolean

    /**
     * Delete all backups from the cloud.
     * @return Number of backups deleted
     */
    suspend fun deleteAllBackups(): Int

    /**
     * Enable automatic sync of backups with the cloud.
     * @param interval The sync interval in milliseconds
     */
    suspend fun enableAutoSync(interval: Long)

    /**
     * Disable automatic sync of backups.
     */
    suspend fun disableAutoSync()

    /**
     * Get the sync status as a flow.
     * @return Flow of sync status updates
     */
    fun getSyncStatus(): Flow<CloudSyncStatus>

    /**
     * Force an immediate sync with the cloud.
     * @return true if sync was successful
     */
    suspend fun forceSync(): Boolean
}
