package com.example.alertapp.config.backup.security

import com.example.alertapp.config.backup.ConfigBackup
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import platform.Foundation.*
import platform.Security.*
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

actual class BackupEncryption actual constructor(
    private val json: Json
) {
    companion object {
        private const val SALT_LENGTH = 16
        private const val IV_LENGTH = 16
        private const val KEY_LENGTH = 32
        private const val ITERATION_COUNT = 10000
        private const val ALGORITHM = kSecKeyAlgorithmAES
    }

    @OptIn(ExperimentalEncodingApi::class)
    actual fun encrypt(backup: ConfigBackup, password: String): EncryptedBackup {
        try {
            // Generate random salt and IV
            val salt = generateRandomBytes(SALT_LENGTH)
            val iv = generateRandomBytes(IV_LENGTH)

            // Derive key from password using PBKDF2
            val key = deriveKey(password, salt)

            // Create encryption parameters
            val algorithm = ALGORITHM
            val options = mapOf(
                kSecAttrKeyType to kSecAttrKeyTypeAES,
                kSecAttrKeySizeInBits to KEY_LENGTH * 8
            )

            // Encrypt data
            val backupJson = json.encodeToString(backup)
            val data = backupJson.encodeToByteArray()
            val encrypted = NSMutableData().apply {
                CCCrypt(
                    op = kCCEncrypt,
                    alg = kCCAlgorithmAES128,
                    options = kCCOptionPKCS7Padding,
                    key = key,
                    keyLength = KEY_LENGTH,
                    iv = iv,
                    dataIn = data,
                    dataInLength = data.size.toULong(),
                    dataOut = this,
                    dataOutAvailable = (data.size + kCCBlockSizeAES128).toULong(),
                    dataOutMoved = null
                )
            }

            // Encode components for storage
            return EncryptedBackup(
                data = Base64.encode(encrypted.toByteArray()),
                salt = Base64.encode(salt),
                iv = Base64.encode(iv)
            )
        } catch (e: Exception) {
            throw BackupSecurityException("Failed to encrypt backup", e)
        }
    }

    @OptIn(ExperimentalEncodingApi::class)
    actual fun decrypt(encrypted: EncryptedBackup, password: String): ConfigBackup {
        try {
            // Decode components
            val encryptedData = Base64.decode(encrypted.data)
            val salt = Base64.decode(encrypted.salt)
            val iv = Base64.decode(encrypted.iv)

            // Derive key from password
            val key = deriveKey(password, salt)

            // Decrypt data
            val decrypted = NSMutableData().apply {
                CCCrypt(
                    op = kCCDecrypt,
                    alg = kCCAlgorithmAES128,
                    options = kCCOptionPKCS7Padding,
                    key = key,
                    keyLength = KEY_LENGTH,
                    iv = iv,
                    dataIn = encryptedData,
                    dataInLength = encryptedData.size.toULong(),
                    dataOut = this,
                    dataOutAvailable = encryptedData.size.toULong(),
                    dataOutMoved = null
                )
            }

            // Parse decrypted data
            val backupJson = decrypted.toByteArray().decodeToString()
            return json.decodeFromString<ConfigBackup>(backupJson)
        } catch (e: Exception) {
            throw BackupSecurityException("Failed to decrypt backup", e)
        }
    }

    private fun deriveKey(password: String, salt: ByteArray): ByteArray {
        val key = ByteArray(KEY_LENGTH)
        CCKeyDerivationPBKDF(
            algorithm = kCCPBKDF2,
            password = password,
            passwordLen = password.length.toULong(),
            salt = salt,
            saltLen = salt.size.toULong(),
            prf = kCCPRFHmacAlgSHA256,
            rounds = ITERATION_COUNT.toUInt(),
            derivedKey = key,
            derivedKeyLen = KEY_LENGTH.toULong()
        )
        return key
    }

    private fun generateRandomBytes(length: Int): ByteArray {
        return ByteArray(length).apply {
            SecRandomCopyBytes(kSecRandomDefault, length.toULong(), this)
        }
    }
}
