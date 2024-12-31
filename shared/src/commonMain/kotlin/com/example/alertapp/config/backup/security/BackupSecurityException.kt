package com.example.alertapp.config.backup.security

/**
 * Exception thrown when backup security operations fail.
 */
class BackupSecurityException(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause)
