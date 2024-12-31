package com.example.alertapp.config.backup.cloud

import com.example.alertapp.config.backup.ConfigBackup
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.datetime.Instant
import kotlinx.serialization.json.Json
import platform.GoogleSignIn.*
import platform.GoogleAPIClientForREST.*

/**
 * iOS implementation of Google Drive backup provider.
 */
actual class GoogleDriveBackupProvider {
    actual val BACKUP_FOLDER_NAME = "AlertAppBackups"
    actual val BACKUP_MIME_TYPE = "application/json"
    
    actual val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    actual val _syncStatus = MutableStateFlow<CloudSyncStatus>(CloudSyncStatus.Disabled)
    private var driveService: GTLRDriveService? = null
    private var backupFolderId: String? = null

    actual suspend fun initialize() {
        if (isAuthenticated()) {
            setupDriveService()
            ensureBackupFolder()
        }
    }

    actual suspend fun isAuthenticated(): Boolean {
        return GIDSignIn.sharedInstance.currentUser?.hasGrantedScope(
            "https://www.googleapis.com/auth/drive.file"
        ) ?: false
    }

    actual suspend fun authenticate(): Boolean {
        // Note: This requires UI interaction
        // Implementation should be coordinated with the UI layer
        return false
    }

    actual suspend fun uploadBackup(backup: ConfigBackup): Boolean {
        try {
            _syncStatus.value = CloudSyncStatus.Syncing(0, "Uploading backup")
            
            val folderId = backupFolderId ?: ensureBackupFolder()
            val backupContent = json.encodeToString(ConfigBackup.serializer(), backup)
            val filename = generateBackupFilename(backup.timestamp)

            val file = GTLRDrive_File().apply {
                this.name = filename
                this.parents = listOf(folderId)
                this.mimeType = BACKUP_MIME_TYPE
            }

            val uploadData = backupContent.encodeToByteArray()
            val uploadParams = GTLRUploadParameters(
                data = uploadData,
                mimeType = BACKUP_MIME_TYPE
            )

            val query = GTLRDriveQuery_FilesCreate.query(
                withObject = file,
                uploadParameters = uploadParams
            )

            driveService?.executeQuery(query)

            _syncStatus.value = CloudSyncStatus.Idle(
                Instant.fromEpochMilliseconds(NSDate.date.timeIntervalSince1970.toLong() * 1000)
            )
            return true
        } catch (e: Exception) {
            _syncStatus.value = CloudSyncStatus.Error(e, null)
            return false
        }
    }

    actual suspend fun downloadBackup(timestamp: Instant): ConfigBackup? {
        try {
            _syncStatus.value = CloudSyncStatus.Syncing(0, "Downloading backup")
            
            val filename = generateBackupFilename(timestamp)
            val query = "name = '$filename' and trashed = false"

            val listQuery = GTLRDriveQuery_FilesList.query().apply {
                this.q = query
                this.spaces = "drive"
            }

            val file = driveService?.executeQuery(listQuery)?.files?.firstOrNull()
            
            return file?.let {
                val downloadQuery = GTLRDriveQuery_FilesGet.queryForMedia(fileId = it.identifier)
                val data = driveService?.executeQuery(downloadQuery)
                
                data?.let { bytes ->
                    val content = bytes.toString(Charsets.UTF_8)
                    json.decodeFromString(ConfigBackup.serializer(), content)
                }
            }?.also {
                _syncStatus.value = CloudSyncStatus.Idle(
                    Instant.fromEpochMilliseconds(NSDate.date.timeIntervalSince1970.toLong() * 1000)
                )
            }
        } catch (e: Exception) {
            _syncStatus.value = CloudSyncStatus.Error(e, null)
            return null
        }
    }

    actual suspend fun listBackups(): List<ConfigBackup> {
        try {
            _syncStatus.value = CloudSyncStatus.Syncing(0, "Listing backups")
            
            val folderId = backupFolderId ?: ensureBackupFolder()
            val query = "'$folderId' in parents and trashed = false"

            val listQuery = GTLRDriveQuery_FilesList.query().apply {
                this.q = query
                this.spaces = "drive"
            }

            return driveService?.executeQuery(listQuery)?.files
                ?.mapNotNull { file ->
                    try {
                        val downloadQuery = GTLRDriveQuery_FilesGet.queryForMedia(
                            fileId = file.identifier
                        )
                        val data = driveService?.executeQuery(downloadQuery)
                        
                        data?.let { bytes ->
                            val content = bytes.toString(Charsets.UTF_8)
                            json.decodeFromString(ConfigBackup.serializer(), content)
                        }
                    } catch (e: Exception) {
                        null
                    }
                }
                ?.sortedByDescending { it.timestamp }
                ?: emptyList()
        } catch (e: Exception) {
            _syncStatus.value = CloudSyncStatus.Error(e, null)
            return emptyList()
        }
    }

    actual suspend fun deleteBackup(backup: ConfigBackup): Boolean {
        try {
            _syncStatus.value = CloudSyncStatus.Syncing(0, "Deleting backup")
            
            val filename = generateBackupFilename(backup.timestamp)
            val query = "name = '$filename' and trashed = false"

            val listQuery = GTLRDriveQuery_FilesList.query().apply {
                this.q = query
                this.spaces = "drive"
            }

            val file = driveService?.executeQuery(listQuery)?.files?.firstOrNull()

            file?.let {
                val deleteQuery = GTLRDriveQuery_FilesDelete.query(
                    fileId = it.identifier
                )
                driveService?.executeQuery(deleteQuery)
            }

            _syncStatus.value = CloudSyncStatus.Idle(
                Instant.fromEpochMilliseconds(NSDate.date.timeIntervalSince1970.toLong() * 1000)
            )
            return true
        } catch (e: Exception) {
            _syncStatus.value = CloudSyncStatus.Error(e, null)
            return false
        }
    }

    actual suspend fun deleteAllBackups(): Int {
        try {
            _syncStatus.value = CloudSyncStatus.Syncing(0, "Deleting all backups")
            
            val folderId = backupFolderId ?: ensureBackupFolder()
            val query = "'$folderId' in parents and trashed = false"

            val listQuery = GTLRDriveQuery_FilesList.query().apply {
                this.q = query
                this.spaces = "drive"
            }

            val files = driveService?.executeQuery(listQuery)?.files ?: return 0

            var count = 0
            files.forEach { file ->
                try {
                    val deleteQuery = GTLRDriveQuery_FilesDelete.query(
                        fileId = file.identifier
                    )
                    driveService?.executeQuery(deleteQuery)
                    count++
                } catch (e: Exception) {
                    // Continue with other deletions
                }
            }

            _syncStatus.value = CloudSyncStatus.Idle(
                Instant.fromEpochMilliseconds(NSDate.date.timeIntervalSince1970.toLong() * 1000)
            )
            return count
        } catch (e: Exception) {
            _syncStatus.value = CloudSyncStatus.Error(e, null)
            return 0
        }
    }

    actual suspend fun enableAutoSync(interval: Long) {
        // Implementation would set up periodic sync using iOS background tasks
        _syncStatus.value = CloudSyncStatus.Idle(
            Instant.fromEpochMilliseconds(NSDate.date.timeIntervalSince1970.toLong() * 1000)
        )
    }

    actual suspend fun disableAutoSync() {
        // Implementation would cancel iOS background tasks
        _syncStatus.value = CloudSyncStatus.Disabled
    }

    actual fun getSyncStatus(): Flow<CloudSyncStatus> = _syncStatus

    actual suspend fun forceSync(): Boolean {
        try {
            _syncStatus.value = CloudSyncStatus.Syncing(0, "Starting sync")
            // Implementation would sync local and remote backups
            _syncStatus.value = CloudSyncStatus.Idle(
                Instant.fromEpochMilliseconds(NSDate.date.timeIntervalSince1970.toLong() * 1000)
            )
            return true
        } catch (e: Exception) {
            _syncStatus.value = CloudSyncStatus.Error(e, null)
            return false
        }
    }

    private fun setupDriveService() {
        val currentUser = GIDSignIn.sharedInstance.currentUser ?: return
        
        driveService = GTLRDriveService().apply {
            this.authorizer = currentUser.authentication.fetcherAuthorizer
        }
    }

    private suspend fun ensureBackupFolder(): String {
        backupFolderId?.let { return it }

        val folderMimeType = "application/vnd.google-apps.folder"
        val query = "mimeType = '$folderMimeType' and name = '$BACKUP_FOLDER_NAME' and trashed = false"

        val listQuery = GTLRDriveQuery_FilesList.query().apply {
            this.q = query
            this.spaces = "drive"
        }

        val folder = driveService?.executeQuery(listQuery)?.files?.firstOrNull()
            ?: run {
                val file = GTLRDrive_File().apply {
                    this.name = BACKUP_FOLDER_NAME
                    this.mimeType = folderMimeType
                }

                val createQuery = GTLRDriveQuery_FilesCreate.query(withObject = file)
                driveService?.executeQuery(createQuery)
            }

        backupFolderId = folder?.identifier
        requireNotNull(backupFolderId) { "Failed to create backup folder" }
        return backupFolderId!!
    }

    private fun generateBackupFilename(timestamp: Instant): String {
        return "config_${timestamp.toString().replace(":", "-")}" +
                ConfigBackup.BACKUP_FILE_EXTENSION
    }
}
