package com.example.alertapp.services.processors

import com.example.alertapp.models.AlertTrigger
import platform.Foundation.NSLog

class IosReleaseAlertProcessor(
    private val releaseService: ReleaseService
) : ReleaseAlertProcessor() {

    override suspend fun checkForNewReleases(trigger: AlertTrigger.ReleaseTrigger): ReleaseResult {
        return try {
            val releases = releaseService.checkNewReleases(
                creator = trigger.creator,
                mediaType = trigger.mediaType,
                conditions = trigger.conditions
            )
            
            ReleaseResult.Success(releases.map { apiRelease ->
                Release(
                    id = apiRelease.id,
                    title = apiRelease.title,
                    creator = apiRelease.creator,
                    mediaType = apiRelease.mediaType,
                    releaseDate = apiRelease.releaseDate,
                    description = apiRelease.description,
                    url = apiRelease.url,
                    metadata = apiRelease.metadata.mapValues { it.value.toString() }
                )
            })
        } catch (e: Exception) {
            logError("Failed to check releases", e)
            ReleaseResult.Error("Failed to check releases: ${e.message}")
        }
    }

    override fun logWarning(message: String) {
        NSLog("⚠️ Release Alert: $message")
    }

    override fun logError(message: String, error: Throwable?) {
        if (error != null) {
            NSLog("❌ Release Alert Error: $message - ${error.message}")
        } else {
            NSLog("❌ Release Alert Error: $message")
        }
    }

    override fun logInfo(message: String) {
        NSLog("ℹ️ Release Alert: $message")
    }
}
