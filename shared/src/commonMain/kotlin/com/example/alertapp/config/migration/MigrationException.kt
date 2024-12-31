package com.example.alertapp.config.migration

/**
 * Exception thrown when a configuration migration fails.
 */
class MigrationException(
    message: String,
    val fromVersion: ConfigVersion,
    val toVersion: ConfigVersion,
    cause: Throwable? = null
) : Exception(message, cause)
