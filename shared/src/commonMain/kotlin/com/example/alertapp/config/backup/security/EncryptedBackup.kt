package com.example.alertapp.config.backup.security

import kotlinx.serialization.Serializable

/**
 * Represents an encrypted backup.
 */
@Serializable
data class EncryptedBackup(
    val data: String,  // Base64 encoded encrypted data
    val salt: String,  // Base64 encoded salt
    val iv: String     // Base64 encoded initialization vector
)
