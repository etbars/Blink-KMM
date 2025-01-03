package com.example.alertapp.config.backup.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import com.example.alertapp.config.backup.ConfigBackup
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec
import kotlin.io.encoding.ExperimentalEncodingApi

actual class BackupEncryption actual constructor(
    private val json: Json
) {
    companion object {
        private const val SALT_LENGTH = 16
        private const val IV_LENGTH = 16
        private const val KEY_LENGTH = 256
        private const val ITERATION_COUNT = 10000
        private const val TRANSFORMATION = "AES/CBC/PKCS7Padding"
    }

    @OptIn(ExperimentalEncodingApi::class)
    actual fun encrypt(backup: ConfigBackup, password: String): EncryptedBackup {
        try {
            // Generate random salt and IV
            val salt = ByteArray(SALT_LENGTH).apply {
                SecureRandom().nextBytes(this)
            }
            val iv = ByteArray(IV_LENGTH).apply {
                SecureRandom().nextBytes(this)
            }

            // Derive key from password using PBKDF2
            val secretKey = deriveKey(password, salt)

            // Initialize cipher
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, IvParameterSpec(iv))

            // Serialize and encrypt the backup
            val backupJson = json.encodeToString(backup)
            val encrypted = cipher.doFinal(backupJson.encodeToByteArray())

            // Encode components for storage
            return EncryptedBackup(
                data = android.util.Base64.encodeToString(encrypted, android.util.Base64.NO_WRAP),
                salt = android.util.Base64.encodeToString(salt, android.util.Base64.NO_WRAP),
                iv = android.util.Base64.encodeToString(iv, android.util.Base64.NO_WRAP)
            )
        } catch (e: Exception) {
            throw BackupSecurityException("Failed to encrypt backup", e)
        }
    }

    @OptIn(ExperimentalEncodingApi::class)
    actual fun decrypt(encrypted: EncryptedBackup, password: String): ConfigBackup {
        try {
            // Decode components
            val encryptedData = android.util.Base64.decode(encrypted.data, android.util.Base64.NO_WRAP)
            val salt = android.util.Base64.decode(encrypted.salt, android.util.Base64.NO_WRAP)
            val iv = android.util.Base64.decode(encrypted.iv, android.util.Base64.NO_WRAP)

            // Derive key from password
            val secretKey = deriveKey(password, salt)

            // Initialize cipher for decryption
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, IvParameterSpec(iv))

            // Decrypt the data
            val decrypted = cipher.doFinal(encryptedData)

            // Parse the decrypted data
            val backupJson = decrypted.decodeToString()
            return json.decodeFromString<ConfigBackup>(backupJson)
        } catch (e: Exception) {
            throw BackupSecurityException("Failed to decrypt backup", e)
        }
    }

    private fun deriveKey(password: String, salt: ByteArray): SecretKeySpec {
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1")
        val spec = PBEKeySpec(password.toCharArray(), salt, ITERATION_COUNT, KEY_LENGTH)
        val tmp = factory.generateSecret(spec)
        return SecretKeySpec(tmp.encoded, "AES")
    }
}
