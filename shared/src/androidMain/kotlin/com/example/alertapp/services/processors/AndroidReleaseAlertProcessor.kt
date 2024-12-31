package com.example.alertapp.services.processors

import com.example.alertapp.models.AlertTrigger
import com.example.alertapp.services.ReleaseService
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AndroidReleaseAlertProcessor @Inject constructor(
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
            ReleaseResult.Error("Failed to check releases: ${e.message}")
        }
    }

    override fun logWarning(message: String) {
        Timber.w(message)
    }

    override fun logError(message: String, error: Throwable?) {
        if (error != null) {
            Timber.e(error, message)
        } else {
            Timber.e(message)
        }
    }

    override fun logInfo(message: String) {
        Timber.i(message)
    }
}
