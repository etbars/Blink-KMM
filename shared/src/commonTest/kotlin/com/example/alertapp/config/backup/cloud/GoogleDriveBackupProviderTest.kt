package com.example.alertapp.config.backup.cloud

import com.example.alertapp.config.backup.ConfigBackup
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.*

class GoogleDriveBackupProviderTest {
    private lateinit var provider: TestGoogleDriveBackupProvider

    @BeforeTest
    fun setup() {
        provider = TestGoogleDriveBackupProvider()
    }

    @Test
    fun testAuthentication() = runTest {
        assertFalse(provider.isAuthenticated())
        
        provider.setAuthenticated(true)
        assertTrue(provider.isAuthenticated())
    }

    @Test
    fun testUploadBackup() = runTest {
        provider.setAuthenticated(true)
        
        val backup = ConfigBackup(
            timestamp = Clock.System.now(),
            version = ConfigVersion.CURRENT,
            appConfig = null,
            platformConfig = null,
            workConfig = null
        )

        assertTrue(provider.uploadBackup(backup))
        assertEquals(CloudSyncStatus.Idle::class, provider.getSyncStatus().first()::class)
    }

    @Test
    fun testUploadFailure() = runTest {
        provider.setAuthenticated(true)
        provider.setShouldFail(true)
        
        val backup = ConfigBackup(
            timestamp = Clock.System.now(),
            version = ConfigVersion.CURRENT,
            appConfig = null,
            platformConfig = null,
            workConfig = null
        )

        assertFalse(provider.uploadBackup(backup))
        assertTrue(provider.getSyncStatus().first() is CloudSyncStatus.Error)
    }

    @Test
    fun testListBackups() = runTest {
        provider.setAuthenticated(true)
        
        val backup1 = ConfigBackup(
            timestamp = Clock.System.now(),
            version = ConfigVersion.CURRENT,
            appConfig = null,
            platformConfig = null,
            workConfig = null
        )
        val backup2 = backup1.copy(
            timestamp = Clock.System.now() + 1.seconds
        )

        provider.uploadBackup(backup1)
        provider.uploadBackup(backup2)

        val backups = provider.listBackups()
        assertEquals(2, backups.size)
        assertTrue(backups[0].timestamp > backups[1].timestamp)
    }

    @Test
    fun testDeleteBackup() = runTest {
        provider.setAuthenticated(true)
        
        val backup = ConfigBackup(
            timestamp = Clock.System.now(),
            version = ConfigVersion.CURRENT,
            appConfig = null,
            platformConfig = null,
            workConfig = null
        )

        provider.uploadBackup(backup)
        assertTrue(provider.deleteBackup(backup))
        assertTrue(provider.listBackups().isEmpty())
    }

    @Test
    fun testAutoSync() = runTest {
        provider.setAuthenticated(true)
        
        provider.enableAutoSync(60_000)
        assertTrue(provider.getSyncStatus().first() is CloudSyncStatus.Idle)

        provider.disableAutoSync()
        assertEquals(CloudSyncStatus.Disabled, provider.getSyncStatus().first())
    }

    private class TestGoogleDriveBackupProvider : CloudBackupProvider {
        private var authenticated = false
        private var shouldFail = false
        private val backups = mutableListOf<ConfigBackup>()
        private val _syncStatus = MutableStateFlow<CloudSyncStatus>(CloudSyncStatus.Disabled)

        fun setAuthenticated(value: Boolean) {
            authenticated = value
        }

        fun setShouldFail(value: Boolean) {
            shouldFail = value
        }

        override suspend fun isAuthenticated(): Boolean = authenticated

        override suspend fun authenticate(): Boolean = true

        override suspend fun uploadBackup(backup: ConfigBackup): Boolean {
            if (!authenticated) return false
            if (shouldFail) {
                _syncStatus.value = CloudSyncStatus.Error(
                    Exception("Test failure"),
                    null
                )
                return false
            }

            _syncStatus.value = CloudSyncStatus.Syncing(50, "Uploading")
            backups.add(backup)
            _syncStatus.value = CloudSyncStatus.Idle(Clock.System.now())
            return true
        }

        override suspend fun downloadBackup(timestamp: Instant): ConfigBackup? {
            if (!authenticated) return null
            if (shouldFail) {
                _syncStatus.value = CloudSyncStatus.Error(
                    Exception("Test failure"),
                    null
                )
                return null
            }

            return backups.firstOrNull { it.timestamp == timestamp }
        }

        override suspend fun listBackups(): List<ConfigBackup> {
            if (!authenticated) return emptyList()
            if (shouldFail) {
                _syncStatus.value = CloudSyncStatus.Error(
                    Exception("Test failure"),
                    null
                )
                return emptyList()
            }

            return backups.sortedByDescending { it.timestamp }
        }

        override suspend fun deleteBackup(backup: ConfigBackup): Boolean {
            if (!authenticated) return false
            if (shouldFail) {
                _syncStatus.value = CloudSyncStatus.Error(
                    Exception("Test failure"),
                    null
                )
                return false
            }

            return backups.removeIf { it.timestamp == backup.timestamp }
        }

        override suspend fun deleteAllBackups(): Int {
            if (!authenticated) return 0
            if (shouldFail) {
                _syncStatus.value = CloudSyncStatus.Error(
                    Exception("Test failure"),
                    null
                )
                return 0
            }

            val count = backups.size
            backups.clear()
            return count
        }

        override suspend fun enableAutoSync(interval: Long) {
            if (!authenticated) return
            _syncStatus.value = CloudSyncStatus.Idle(Clock.System.now())
        }

        override suspend fun disableAutoSync() {
            _syncStatus.value = CloudSyncStatus.Disabled
        }

        override fun getSyncStatus(): Flow<CloudSyncStatus> = _syncStatus

        override suspend fun forceSync(): Boolean {
            if (!authenticated) return false
            if (shouldFail) {
                _syncStatus.value = CloudSyncStatus.Error(
                    Exception("Test failure"),
                    null
                )
                return false
            }

            _syncStatus.value = CloudSyncStatus.Syncing(100, "Syncing")
            _syncStatus.value = CloudSyncStatus.Idle(Clock.System.now())
            return true
        }
    }
}
