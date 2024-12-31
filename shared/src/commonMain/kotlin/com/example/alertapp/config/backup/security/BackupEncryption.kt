package com.example.alertapp.config.backup.security

import com.example.alertapp.config.backup.ConfigBackup
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.kotlincrypto.hash.sha2.SHA256
import org.kotlincrypto.symmetric.cipher.Block
import org.kotlincrypto.symmetric.cipher.aes.AES
import org.kotlincrypto.symmetric.cipher.aes.AESMode
import org.kotlincrypto.symmetric.keychain.KeyDerivation
import org.kotlincrypto.symmetric.keychain.PBKDF2
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

/**
 * Handles encryption and decryption of backups.
 */
class BackupEncryption(
    private val json: Json = Json { prettyPrint = true }
) {
    companion object {
        private const val SALT_LENGTH = 16
        private const val IV_LENGTH = 16
        private const val KEY_LENGTH = 32
        private const val ITERATION_COUNT = 10000
    }

    /**
     * Encrypt a backup using a password.
     * @param backup The backup to encrypt
     * @param password The password to use for encryption
     * @return The encrypted backup data
     */
    @OptIn(ExperimentalEncodingApi::class)
    fun encrypt(backup: ConfigBackup, password: String): EncryptedBackup {
        // Generate random salt and IV
        val salt = generateRandomBytes(SALT_LENGTH)
        val iv = generateRandomBytes(IV_LENGTH)

        // Derive key from password
        val key = deriveKey(password, salt)

        // Serialize and encrypt the backup
        val backupJson = json.encodeToString(backup)
        val cipher = createCipher(key, iv, true)
        val encrypted = cipher.doFinal(backupJson.encodeToByteArray())

        // Encode components for storage
        return EncryptedBackup(
            data = Base64.encode(encrypted),
            salt = Base64.encode(salt),
            iv = Base64.encode(iv)
        )
    }

    /**
     * Decrypt a backup using a password.
     * @param encrypted The encrypted backup data
     * @param password The password used for encryption
     * @return The decrypted backup
     * @throws BackupSecurityException if decryption fails
     */
    @OptIn(ExperimentalEncodingApi::class)
    fun decrypt(encrypted: EncryptedBackup, password: String): ConfigBackup {
        try {
            // Decode components
            val encryptedData = Base64.decode(encrypted.data)
            val salt = Base64.decode(encrypted.salt)
            val iv = Base64.decode(encrypted.iv)

            // Derive key from password
            val key = deriveKey(password, salt)

            // Decrypt the backup
            val cipher = createCipher(key, iv, false)
            val decrypted = cipher.doFinal(encryptedData)

            // Deserialize the backup
            return json.decodeFromString(decrypted.decodeToString())
        } catch (e: Exception) {
            throw BackupSecurityException("Failed to decrypt backup", e)
        }
    }

    /**
     * Create an AES cipher for encryption or decryption.
     */
    private fun createCipher(key: ByteArray, iv: ByteArray, encrypt: Boolean): Block {
        return AES(key, AESMode.CBC).apply {
            if (encrypt) {
                encrypt(iv)
            } else {
                decrypt(iv)
            }
        }
    }

    /**
     * Derive an encryption key from a password using PBKDF2.
     */
    private fun deriveKey(password: String, salt: ByteArray): ByteArray {
        val kdf = PBKDF2(
            mac = SHA256,
            iterations = ITERATION_COUNT,
            outputLength = KEY_LENGTH
        )
        return kdf.derive(password.encodeToByteArray(), salt)
    }

    /**
     * Generate random bytes for salt or IV.
     */
    private fun generateRandomBytes(length: Int): ByteArray {
        return ByteArray(length).apply {
            kotlin.random.Random.nextBytes(this)
        }
    }
}
