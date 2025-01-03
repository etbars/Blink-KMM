package com.example.alertapp.config.backup.security

import com.example.alertapp.config.backup.ConfigBackup
import com.example.alertapp.config.backup.security.EncryptedBackup
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

/**
 * Handles encryption and decryption of backups.
 */
expect class BackupEncryption(json: Json = Json { prettyPrint = true }) {
    /**
     * Encrypt a backup using a password.
     * @param backup The backup to encrypt
     * @param password The password to use for encryption
     * @return The encrypted backup data
     */
    @OptIn(ExperimentalEncodingApi::class)
    fun encrypt(backup: ConfigBackup, password: String): EncryptedBackup

    /**
     * Decrypt a backup using a password.
     * @param encrypted The encrypted backup data
     * @param password The password to use for decryption
     * @return The decrypted backup
     */
    @OptIn(ExperimentalEncodingApi::class)
    fun decrypt(encrypted: EncryptedBackup, password: String): ConfigBackup
}
