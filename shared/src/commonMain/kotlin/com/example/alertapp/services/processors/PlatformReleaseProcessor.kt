package com.example.alertapp.services.processors

import com.example.alertapp.models.Release
import kotlinx.datetime.Instant

interface PlatformReleaseProcessor {
    suspend fun checkNewReleases(
        creator: String?,
        type: String,
        since: Instant? = null
    ): List<Release>

    fun logWarning(message: String)
    fun logError(message: String, error: Throwable? = null)
    fun logInfo(message: String)
}
