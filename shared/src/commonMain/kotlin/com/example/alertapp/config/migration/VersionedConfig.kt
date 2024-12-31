package com.example.alertapp.config.migration

import kotlinx.serialization.Serializable

/**
 * Wrapper for configuration data that includes version information.
 */
@Serializable
data class VersionedConfig<T>(
    val version: ConfigVersion,
    val data: T
)
