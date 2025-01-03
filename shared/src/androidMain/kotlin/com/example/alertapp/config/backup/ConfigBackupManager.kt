package com.example.alertapp.config.backup

import android.content.Context
import com.example.alertapp.config.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.encodeToJsonElement
import okio.FileSystem
import okio.Path.Companion.toPath
import javax.inject.Inject
import javax.inject.Singleton
import com.example.alertapp.config.migration.ConfigVersion

/**
 * Android implementation of ConfigBackupManager.
 */
@Singleton
actual class ConfigBackupManager @Inject constructor(
    private val context: Context,
    private val configManager: ConfigManager,
    private val json: Json,
    private val fileSystem: FileSystem
) {
    private val backupDir = "${context.filesDir}/config_backups"

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
                if (backup.appConfig != null) {
                    val appConfig = json.decodeFromJsonElement(AlertAppConfig.serializer(), backup.appConfig)
                    configManager.updateAppConfig(appConfig)
                }
                if (backup.platformConfig != null) {
                    val platformConfig = json.decodeFromJsonElement(PlatformConfig.serializer(), backup.platformConfig)
                    configManager.updatePlatformConfig(platformConfig)
                }
                if (backup.workConfig != null) {
                    val workConfig = json.decodeFromJsonElement(WorkConfig.serializer(), backup.workConfig)
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
                .filter { it.name.endsWith(".json") }
                .mapNotNull { path ->
                    runCatching {
                        val content = fileSystem.read(path) {
                            readUtf8()
                        }
                        json.decodeFromString(ConfigBackup.serializer(), content)
                    }.getOrNull()
                }
                .sortedByDescending { it.timestamp }
        }

    actual suspend fun deleteBackup(backup: ConfigBackup): Boolean =
        withContext(Dispatchers.IO) {
            val filename = generateBackupFilename(backup.timestamp)
            val path = "$backupDir/$filename".toPath()
            runCatching { fileSystem.delete(path) }.isSuccess
        }

    actual suspend fun deleteAllBackups(): Int =
        withContext(Dispatchers.IO) {
            var count = 0
            fileSystem.list(backupDir.toPath())
                .filter { it.name.endsWith(".json") }
                .forEach { path ->
                    if (runCatching { fileSystem.delete(path) }.isSuccess) {
                        count++
                    }
                }
            count
        }

    private fun createBackupFromConfig(
        appConfig: AlertAppConfig?,
        platformConfig: PlatformConfig?,
        workConfig: WorkConfig?,
        metadata: Map<String, String> = emptyMap()
    ): ConfigBackup = ConfigBackup(
        timestamp = Clock.System.now(),
        version = ConfigVersion.CURRENT,
        appConfig = appConfig?.let { json.encodeToJsonElement(it) as JsonObject },
        platformConfig = platformConfig?.let { json.encodeToJsonElement(it) as JsonObject },
        workConfig = workConfig?.let { json.encodeToJsonElement(it) as JsonObject },
        metadata = metadata
    )

    private fun generateBackupFilename(timestamp: Instant): String =
        "backup_${timestamp.toString().replace(":", "-")}.json"

    private fun cleanupOldBackups(
        backups: List<ConfigBackup>,
        fileSystem: FileSystem,
        backupDir: String
    ) {
        // Keep only the most recent backups
        if (backups.size > ConfigBackup.MAX_BACKUPS) {
            backups.drop(ConfigBackup.MAX_BACKUPS).forEach { backup ->
                val filename = generateBackupFilename(backup.timestamp)
                val path = "$backupDir/$filename".toPath()
                runCatching { fileSystem.delete(path) }
            }
        }
    }
}
