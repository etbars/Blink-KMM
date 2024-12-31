package com.example.alertapp.config.backup.cloud

import android.content.Context
import com.example.alertapp.config.backup.ConfigBackup
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.ByteArrayContent
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withContext
import kotlinx.datetime.Instant
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Android implementation of Google Drive backup provider.
 */
@Singleton
actual class GoogleDriveBackupProvider @Inject constructor(
    private val context: Context
) : CloudBackupProvider {
    actual val BACKUP_FOLDER_NAME = "AlertAppBackups"
    actual val BACKUP_MIME_TYPE = "application/json"
    
    actual val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    actual val _syncStatus = MutableStateFlow<CloudSyncStatus>(CloudSyncStatus.Disabled)
    private var driveService: Drive? = null
    private var backupFolderId: String? = null

    actual suspend fun initialize() {
        withContext(Dispatchers.IO) {
            if (isAuthenticated()) {
                setupDriveService()
                ensureBackupFolder()
            }
        }
    }

    actual override suspend fun isAuthenticated(): Boolean = withContext(Dispatchers.IO) {
        GoogleSignIn.getLastSignedInAccount(context)?.let { account ->
            account.grantedScopes.contains(Scope(DriveScopes.DRIVE_FILE))
        } ?: false
    }

    actual override suspend fun authenticate(): Boolean = withContext(Dispatchers.IO) {
        try {
            val signInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestScopes(Scope(DriveScopes.DRIVE_FILE))
                .build()

            val signInClient = GoogleSignIn.getClient(context, signInOptions)
            // Note: This requires activity context and result handling
            // Implementation should be coordinated with the UI layer
            false // Return false as we need UI interaction
        } catch (e: Exception) {
            false
        }
    }

    actual override suspend fun uploadBackup(backup: ConfigBackup): Boolean =
        withContext(Dispatchers.IO) {
            try {
                _syncStatus.value = CloudSyncStatus.Syncing(0, "Uploading backup")
                
                val folderId = backupFolderId ?: ensureBackupFolder()
                val backupContent = json.encodeToString(ConfigBackup.serializer(), backup)
                val filename = generateBackupFilename(backup.timestamp)

                val fileMetadata = com.google.api.services.drive.model.File().apply {
                    name = filename
                    parents = listOf(folderId)
                    mimeType = BACKUP_MIME_TYPE
                }

                val content = ByteArrayContent.fromString(BACKUP_MIME_TYPE, backupContent)
                
                driveService?.files()?.create(fileMetadata, content)
                    ?.setFields("id, name, createdTime")
                    ?.execute()

                _syncStatus.value = CloudSyncStatus.Idle(Instant.fromEpochMilliseconds(System.currentTimeMillis()))
                true
            } catch (e: Exception) {
                _syncStatus.value = CloudSyncStatus.Error(e, null)
                false
            }
        }

    actual override suspend fun downloadBackup(timestamp: Instant): ConfigBackup? =
        withContext(Dispatchers.IO) {
            try {
                _syncStatus.value = CloudSyncStatus.Syncing(0, "Downloading backup")
                
                val filename = generateBackupFilename(timestamp)
                val query = "name = '$filename' and trashed = false"

                val file = driveService?.files()?.list()
                    ?.setQ(query)
                    ?.setSpaces("drive")
                    ?.execute()
                    ?.files
                    ?.firstOrNull()

                file?.let { 
                    driveService?.files()?.get(it.id)
                        ?.executeMediaAsInputStream()
                        ?.use { stream ->
                            val content = stream.bufferedReader().use { it.readText() }
                            json.decodeFromString(ConfigBackup.serializer(), content)
                        }
                }?.also {
                    _syncStatus.value = CloudSyncStatus.Idle(
                        Instant.fromEpochMilliseconds(System.currentTimeMillis())
                    )
                }
            } catch (e: Exception) {
                _syncStatus.value = CloudSyncStatus.Error(e, null)
                null
            }
        }

    actual override suspend fun listBackups(): List<ConfigBackup> =
        withContext(Dispatchers.IO) {
            try {
                _syncStatus.value = CloudSyncStatus.Syncing(0, "Listing backups")
                
                val folderId = backupFolderId ?: ensureBackupFolder()
                val query = "'$folderId' in parents and trashed = false"

                driveService?.files()?.list()
                    ?.setQ(query)
                    ?.setSpaces("drive")
                    ?.execute()
                    ?.files
                    ?.mapNotNull { file ->
                        try {
                            driveService?.files()?.get(file.id)
                                ?.executeMediaAsInputStream()
                                ?.use { stream ->
                                    val content = stream.bufferedReader().use { it.readText() }
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
                emptyList()
            }
        }

    actual override suspend fun deleteBackup(backup: ConfigBackup): Boolean =
        withContext(Dispatchers.IO) {
            try {
                _syncStatus.value = CloudSyncStatus.Syncing(0, "Deleting backup")
                
                val filename = generateBackupFilename(backup.timestamp)
                val query = "name = '$filename' and trashed = false"

                val file = driveService?.files()?.list()
                    ?.setQ(query)
                    ?.setSpaces("drive")
                    ?.execute()
                    ?.files
                    ?.firstOrNull()

                file?.let {
                    driveService?.files()?.delete(it.id)?.execute()
                }

                _syncStatus.value = CloudSyncStatus.Idle(
                    Instant.fromEpochMilliseconds(System.currentTimeMillis())
                )
                true
            } catch (e: Exception) {
                _syncStatus.value = CloudSyncStatus.Error(e, null)
                false
            }
        }

    actual override suspend fun deleteAllBackups(): Int = withContext(Dispatchers.IO) {
        try {
            _syncStatus.value = CloudSyncStatus.Syncing(0, "Deleting all backups")
            
            val folderId = backupFolderId ?: ensureBackupFolder()
            val query = "'$folderId' in parents and trashed = false"

            val files = driveService?.files()?.list()
                ?.setQ(query)
                ?.setSpaces("drive")
                ?.execute()
                ?.files ?: return@withContext 0

            var count = 0
            files.forEach { file ->
                try {
                    driveService?.files()?.delete(file.id)?.execute()
                    count++
                } catch (e: Exception) {
                    // Continue with other deletions
                }
            }

            _syncStatus.value = CloudSyncStatus.Idle(
                Instant.fromEpochMilliseconds(System.currentTimeMillis())
            )
            count
        } catch (e: Exception) {
            _syncStatus.value = CloudSyncStatus.Error(e, null)
            0
        }
    }

    actual override suspend fun enableAutoSync(interval: Long) {
        // Implementation would set up WorkManager for periodic sync
        _syncStatus.value = CloudSyncStatus.Idle(
            Instant.fromEpochMilliseconds(System.currentTimeMillis())
        )
    }

    actual override suspend fun disableAutoSync() {
        // Implementation would cancel WorkManager periodic sync
        _syncStatus.value = CloudSyncStatus.Disabled
    }

    actual override fun getSyncStatus(): Flow<CloudSyncStatus> = _syncStatus

    actual override suspend fun forceSync(): Boolean = withContext(Dispatchers.IO) {
        try {
            _syncStatus.value = CloudSyncStatus.Syncing(0, "Starting sync")
            // Implementation would sync local and remote backups
            _syncStatus.value = CloudSyncStatus.Idle(
                Instant.fromEpochMilliseconds(System.currentTimeMillis())
            )
            true
        } catch (e: Exception) {
            _syncStatus.value = CloudSyncStatus.Error(e, null)
            false
        }
    }

    private suspend fun setupDriveService() {
        val account = GoogleSignIn.getLastSignedInAccount(context) ?: return
        
        val credential = GoogleAccountCredential.usingOAuth2(
            context,
            listOf(DriveScopes.DRIVE_FILE)
        ).apply {
            selectedAccount = account.account
        }

        driveService = Drive.Builder(
            NetHttpTransport(),
            GsonFactory(),
            credential
        )
            .setApplicationName("AlertApp")
            .build()
    }

    private suspend fun ensureBackupFolder(): String = withContext(Dispatchers.IO) {
        backupFolderId?.let { return@withContext it }

        val folderMimeType = "application/vnd.google-apps.folder"
        val query = "mimeType = '$folderMimeType' and name = '$BACKUP_FOLDER_NAME' and trashed = false"

        val folder = driveService?.files()?.list()
            ?.setQ(query)
            ?.setSpaces("drive")
            ?.execute()
            ?.files
            ?.firstOrNull()
            ?: driveService?.files()?.create(
                com.google.api.services.drive.model.File().apply {
                    name = BACKUP_FOLDER_NAME
                    mimeType = folderMimeType
                }
            )
            ?.setFields("id")
            ?.execute()

        backupFolderId = folder?.id
        requireNotNull(backupFolderId) { "Failed to create backup folder" }
    }

    private fun generateBackupFilename(timestamp: Instant): String {
        return "config_${timestamp.toString().replace(":", "-")}" +
                ConfigBackup.BACKUP_FILE_EXTENSION
    }
}
