package com.example.alertapp.services.processors

import com.example.alertapp.api.ApiResponse
import com.example.alertapp.enums.ReleaseType
import com.example.alertapp.models.Alert
import com.example.alertapp.models.AlertTrigger
import com.example.alertapp.models.ProcessingResult
import com.example.alertapp.models.Release
import com.example.alertapp.api.release.ReleaseProvider
import co.touchlab.kermit.Logger

class ReleaseAlertProcessor(
    private val releaseProvider: ReleaseProvider,
    private val logger: Logger = Logger.withTag("ReleaseAlertProcessor")
) {
    private suspend fun checkNewReleases(
        platform: String,
        category: String? = null,
        minRating: Double? = null
    ): ReleaseResult {
        return when (val response = releaseProvider.getReleases(
            platform = platform,
            category = category,
            minRating = minRating
        )) {
            is ApiResponse.Success -> {
                val releases = response.data
                ReleaseResult.Success(releases)
            }
            is ApiResponse.Error -> {
                ReleaseResult.Error(response.error.message)
            }
            is ApiResponse.Loading -> {
                ReleaseResult.Loading
            }
        }
    }

    suspend fun processAlert(alert: Alert): ProcessingResult {
        val trigger = alert.trigger as? AlertTrigger.ReleaseTrigger
            ?: return ProcessingResult.Error(
                message = "Invalid trigger type",
                code = "INVALID_TRIGGER"
            )

        return try {
            when (val result = checkNewReleases(
                platform = trigger.type,
                category = trigger.creator,
                minRating = trigger.minRating
            )) {
                is ReleaseResult.Success -> {
                    val releases = result.releases.filter { release ->
                        trigger.conditions.all { condition ->
                            when (condition) {
                                "hasRating" -> release.rating != null
                                "hasDescription" -> !release.description.isNullOrBlank()
                                "hasUrl" -> !release.url.isNullOrBlank()
                                else -> true
                            }
                        }
                    }

                    if (releases.isNotEmpty()) {
                        ProcessingResult.Triggered(
                            message = "New releases found: ${releases.size}",
                            metadata = mapOf(
                                "platform" to trigger.type,
                                "category" to (trigger.creator ?: "all"),
                                "count" to releases.size.toString(),
                                "latest" to releases.first().title
                            )
                        )
                    } else {
                        ProcessingResult.NotTriggered(
                            message = "No new releases found matching criteria"
                        )
                    }
                }
                is ReleaseResult.Error -> {
                    ProcessingResult.Error(
                        message = result.message,
                        code = "RELEASE_ERROR"
                    )
                }
                ReleaseResult.Loading -> {
                    ProcessingResult.NotTriggered(
                        message = "Processing release data"
                    )
                }
            }
        } catch (e: Exception) {
            logger.e("Failed to process release alert", e)
            ProcessingResult.Error(
                message = "Failed to process release alert: ${e.message}",
                code = "PROCESSING_ERROR"
            )
        }
    }

    private fun logWarning(message: String) {
        logger.w(message)
    }

    private fun logError(message: String, error: Throwable? = null) {
        if (error != null) {
            logger.e(message, error)
        } else {
            logger.e(message)
        }
    }

    private fun logInfo(message: String) {
        logger.i(message)
    }
}

sealed class ReleaseResult {
    data class Success(val releases: List<Release>) : ReleaseResult()
    data class Error(val message: String) : ReleaseResult()
    object Loading : ReleaseResult()
}
