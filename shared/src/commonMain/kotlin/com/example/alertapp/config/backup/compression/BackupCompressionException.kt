package com.example.alertapp.config.backup.compression

/**
 * Exception thrown when backup compression operations fail.
 */
class BackupCompressionException(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause)
