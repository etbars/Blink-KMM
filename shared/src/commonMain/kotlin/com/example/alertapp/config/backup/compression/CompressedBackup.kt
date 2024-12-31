package com.example.alertapp.config.backup.compression

import kotlinx.serialization.Serializable

/**
 * Represents a compressed backup.
 */
@Serializable
data class CompressedBackup(
    val data: String  // Base64 encoded compressed data
)
