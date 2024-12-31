package com.example.alertapp.config.backup

import com.example.alertapp.config.*
import com.example.alertapp.config.migration.ConfigVersion
import kotlinx.datetime.Clock
import kotlinx.serialization.json.*
import okio.Path.Companion.toPath
import okio.fakefilesystem.FakeFileSystem
import kotlin.test.*

class ConfigBackupManagerTest {
    private lateinit var fileSystem: FakeFileSystem
    private lateinit var backupDir: String
    private val json = Json { 
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    @BeforeTest
    fun setup() {
        fileSystem = FakeFileSystem()
        backupDir = "/config_backups"
        fileSystem.createDirectories(backupDir.toPath())
    }

    @Test
    fun testBackupCreation() {
        val backup = ConfigBackupManager.createBackupFromConfig(
            appConfig = AlertAppConfig(),
            platformConfig = PlatformConfig(),
            workConfig = WorkConfig(),
            metadata = mapOf("reason" to "test")
        )

        assertNotNull(backup.appConfig)
        assertNotNull(backup.platformConfig)
        assertNotNull(backup.workConfig)
        assertEquals("test", backup.metadata["reason"])
        assertEquals(ConfigVersion.CURRENT, backup.version)
    }

    @Test
    fun testBackupFilenameGeneration() {
        val timestamp = Clock.System.now()
        val filename = ConfigBackupManager.generateBackupFilename(timestamp)
        
        assertTrue(filename.startsWith("config_"))
        assertTrue(filename.endsWith(ConfigBackup.BACKUP_FILE_EXTENSION))
        assertFalse(filename.contains(":"))
    }

    @Test
    fun testOldBackupCleanup() {
        // Create more than MAX_BACKUPS backups
        val backups = (1..ConfigBackup.MAX_BACKUPS + 3).map { i ->
            val backup = ConfigBackupManager.createBackupFromConfig(
                appConfig = AlertAppConfig(),
                platformConfig = null,
                workConfig = null
            )
            
            val filename = ConfigBackupManager.generateBackupFilename(backup.timestamp)
            val path = "$backupDir/$filename".toPath()
            fileSystem.write(path) {
                writeUtf8(json.encodeToString(ConfigBackup.serializer(), backup))
            }
            
            backup
        }

        runTest {
            val deletedCount = ConfigBackupManager.cleanupOldBackups(
                backups,
                fileSystem,
                backupDir
            )

            assertEquals(3, deletedCount)
            assertEquals(
                ConfigBackup.MAX_BACKUPS,
                fileSystem.list(backupDir.toPath()).size
            )
        }
    }

    @Test
    fun testPartialBackupCreation() {
        val backup = ConfigBackupManager.createBackupFromConfig(
            appConfig = AlertAppConfig(),
            platformConfig = null,
            workConfig = null
        )

        assertNotNull(backup.appConfig)
        assertNull(backup.platformConfig)
        assertNull(backup.workConfig)
    }

    @Test
    fun testBackupWithMetadata() {
        val metadata = mapOf(
            "reason" to "test",
            "user" to "john",
            "environment" to "staging"
        )

        val backup = ConfigBackupManager.createBackupFromConfig(
            appConfig = AlertAppConfig(),
            platformConfig = PlatformConfig(),
            workConfig = WorkConfig(),
            metadata = metadata
        )

        assertEquals(metadata, backup.metadata)
    }
}
