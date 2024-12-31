package com.example.alertapp.config.backup.security

import com.example.alertapp.config.AlertAppConfig
import com.example.alertapp.config.backup.ConfigBackup
import com.example.alertapp.config.migration.ConfigVersion
import kotlinx.datetime.Clock
import kotlin.test.*

class BackupEncryptionTest {
    private lateinit var encryption: BackupEncryption
    private lateinit var testBackup: ConfigBackup
    private val testPassword = "test_password_123"

    @BeforeTest
    fun setup() {
        encryption = BackupEncryption()
        testBackup = ConfigBackup(
            timestamp = Clock.System.now(),
            version = ConfigVersion.CURRENT,
            appConfig = AlertAppConfig(),
            platformConfig = null,
            workConfig = null
        )
    }

    @Test
    fun testEncryptionDecryption() {
        // Encrypt backup
        val encrypted = encryption.encrypt(testBackup, testPassword)
        
        // Verify encrypted data is not null and different from original
        assertNotNull(encrypted.data)
        assertNotNull(encrypted.salt)
        assertNotNull(encrypted.iv)
        
        // Decrypt backup
        val decrypted = encryption.decrypt(encrypted, testPassword)
        
        // Verify decrypted data matches original
        assertEquals(testBackup.timestamp, decrypted.timestamp)
        assertEquals(testBackup.version, decrypted.version)
        assertEquals(testBackup.appConfig, decrypted.appConfig)
        assertEquals(testBackup.platformConfig, decrypted.platformConfig)
        assertEquals(testBackup.workConfig, decrypted.workConfig)
    }

    @Test
    fun testDecryptionWithWrongPassword() {
        val encrypted = encryption.encrypt(testBackup, testPassword)
        
        assertFailsWith<BackupSecurityException> {
            encryption.decrypt(encrypted, "wrong_password")
        }
    }

    @Test
    fun testDecryptionWithCorruptedData() {
        val encrypted = encryption.encrypt(testBackup, testPassword)
        val corrupted = encrypted.copy(data = encrypted.data.dropLast(1))
        
        assertFailsWith<BackupSecurityException> {
            encryption.decrypt(corrupted, testPassword)
        }
    }

    @Test
    fun testMultipleEncryptions() {
        val encrypted1 = encryption.encrypt(testBackup, testPassword)
        val encrypted2 = encryption.encrypt(testBackup, testPassword)
        
        // Verify different salt and IV are used
        assertNotEquals(encrypted1.salt, encrypted2.salt)
        assertNotEquals(encrypted1.iv, encrypted2.iv)
        
        // Verify both can be decrypted
        val decrypted1 = encryption.decrypt(encrypted1, testPassword)
        val decrypted2 = encryption.decrypt(encrypted2, testPassword)
        
        assertEquals(decrypted1, decrypted2)
    }
}
