package com.example.alertapp.config.migration

import kotlinx.serialization.Serializable

/**
 * Represents a configuration version.
 * Used to track and manage configuration migrations.
 */
@Serializable
data class ConfigVersion(
    val major: Int,
    val minor: Int,
    val patch: Int
) : Comparable<ConfigVersion> {
    override fun compareTo(other: ConfigVersion): Int {
        return when {
            major != other.major -> major.compareTo(other.major)
            minor != other.minor -> minor.compareTo(other.minor)
            else -> patch.compareTo(other.patch)
        }
    }

    override fun toString(): String = "$major.$minor.$patch"

    companion object {
        val CURRENT = ConfigVersion(1, 0, 0)
        
        fun fromString(version: String): ConfigVersion {
            val parts = version.split(".")
            require(parts.size == 3) { "Invalid version format: $version" }
            return ConfigVersion(
                major = parts[0].toInt(),
                minor = parts[1].toInt(),
                patch = parts[2].toInt()
            )
        }
    }
}
