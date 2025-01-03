package com.example.alertapp.services.processors

import com.example.alertapp.api.ApiResponse
import com.example.alertapp.api.release.ReleaseProvider
import com.example.alertapp.models.Release
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.datetime.Instant

@Singleton
class AndroidReleaseAlertProcessor @Inject constructor(
    private val releaseProvider: ReleaseProvider
) {
    suspend fun checkReleases(
        platform: String,
        category: String? = null,
        since: Instant? = null
    ): List<Release> {
        try {
            Timber.i("Checking for new releases in category $category on platform $platform")
            
            val response = releaseProvider.getReleases(
                platform = platform,
                category = category,
                since = since
            )

            return when (response) {
                is ApiResponse.Success -> {
                    response.data
                }
                is ApiResponse.Error -> {
                    Timber.e(response.error, "Error fetching releases")
                    throw RuntimeException(response.error.message)
                }
                is ApiResponse.Loading -> {
                    emptyList()
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Error checking releases")
            throw e
        }
    }

    fun logWarning(message: String) {
        Timber.w(message)
    }

    fun logError(message: String, error: Throwable?) {
        if (error != null) {
            Timber.e(error, message)
        } else {
            Timber.e(message)
        }
    }

    fun logInfo(message: String) {
        Timber.i(message)
    }
}
