package com.example.alertapp.config.backup

import android.content.Context
import com.example.alertapp.config.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okio.FileSystem
import okio.Path.Companion.toPath
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Android implementation of ConfigBackupManager.
 */
@Singleton
actual class ConfigBackupManager @Inject constructor(
    private val context: Context,
    private val configManager: ConfigManager
) {
    private val backupDir = "${context.filesDir}/config_backups"
    private val fileSystem = FileSystem.SYSTEM
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    init {
        // Ensure backup directory exists
        fileSystem.createDirectories(backupDir.toPath())
    }

    actual suspend fun createBackup(metadata: Map<String, String>): ConfigBackup =
        withContext(Dispatchers.IO) {
            val backup = createBackupFromConfig(
                appConfig = runCatching { configManager.getAppConfig() }.getOrNull(),
                platformConfig = runCatching { configManager.getPlatformConfig() }.getOrNull(),
                workConfig = runCatching { configManager.getWorkConfig() }.getOrNull(),
                metadata = metadata
            )

            val filename = generateBackupFilename(backup.timestamp)
            val path = "$backupDir/$filename".toPath()
            
            fileSystem.write(path) {
                writeUtf8(json.encodeToString(ConfigBackup.serializer(), backup))
            }

            // Clean up old backups
            cleanupOldBackups(listBackups(), fileSystem, backupDir)

            backup
        }

    actual suspend fun restoreBackup(backup: ConfigBackup): Boolean =
        withContext(Dispatchers.IO) {
            try {
                backup.appConfig?.let { config ->
                    val appConfig = json.decodeFromJsonElement(
                        AlertAppConfig.serializer(), 
                        config
                    )
                    configManager.updateAppConfig(appConfig)
                }

                backup.platformConfig?.let { config ->
                    val platformConfig = json.decodeFromJsonElement(
                        PlatformConfig.serializer(), 
                        config
                    )
                    configManager.updatePlatformConfig(platformConfig)
                }

                backup.workConfig?.let { config ->
                    val workConfig = json.decodeFromJsonElement(
                        WorkConfig.serializer(), 
                        config
                    )
                    configManager.updateWorkConfig(workConfig)
                }

                true
            } catch (e: Exception) {
                false
            }
        }

    actual suspend fun listBackups(): List<ConfigBackup> =
        withContext(Dispatchers.IO) {
            fileSystem.list(backupDir.toPath())
                .filter { it.name.endsWith(ConfigBackup.BACKUP_FILE_EXTENSION) }
                .mapNotNull { path ->
                    try {
                        val content = fileSystem.read(path) {
                            readUtf8()
                        }
                        json.decodeFromString(ConfigBackup.serializer(), content)
                    } catch (e: Exception) {
                        null
                    }
                }
                .sortedByDescending { it.timestamp }
        }

    actual suspend fun deleteBackup(backup: ConfigBackup): Boolean =
        withContext(Dispatchers.IO) {
            try {
                val filename = generateBackupFilename(backup.timestamp)
                val path = "$backupDir/$filename".toPath()
                fileSystem.delete(path)
                true
            } catch (e: Exception) {
                false
            }
        }

    actual suspend fun deleteAllBackups(): Int =
        withContext(Dispatchers.IO) {
            var count = 0
            fileSystem.list(backupDir.toPath())
                .filter { it.name.endsWith(ConfigBackup.BACKUP_FILE_EXTENSION) }
                .forEach { path ->
                    try {
                        fileSystem.delete(path)
                        count++
                    } catch (e: Exception) {
                        // Continue with other deletions
                    }
                }
            count
        }
}
