package com.example.alertapp.config.backup.cloud

import kotlinx.datetime.Instant

/**
 * Represents the current status of cloud sync.
 */
sealed class CloudSyncStatus {
    /**
     * Cloud sync is disabled.
     */
    object Disabled : CloudSyncStatus()

    /**
     * Cloud sync is enabled but not currently syncing.
     * @param lastSync The timestamp of the last successful sync
     * @param pendingChanges Number of changes pending upload
     */
    data class Idle(
        val lastSync: Instant?,
        val pendingChanges: Int = 0
    ) : CloudSyncStatus() {
        companion object {
            fun create() = Idle(null, 0)
        }
    }

    /**
     * Cloud sync is currently in progress.
     * @param currentOperation Description of the current operation
     */
    data class Syncing(
        val currentOperation: String,
        val progress: Int = 0
    ) : CloudSyncStatus()

    /**
     * Cloud sync encountered an error.
     * @param error The error that occurred
     * @param lastSync The timestamp of the last successful sync
     */
    data class Error(
        val error: Throwable,
        val lastSync: Instant?
    ) : CloudSyncStatus()
}
