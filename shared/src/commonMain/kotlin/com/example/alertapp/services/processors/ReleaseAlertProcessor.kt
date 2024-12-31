package com.example.alertapp.services.processors

import com.example.alertapp.models.*
import com.example.alertapp.services.base.*
import kotlinx.serialization.Serializable

abstract class ReleaseAlertProcessor : BaseAlertProcessor() {

    override val supportedType: AlertType = AlertType.RELEASE

    override suspend fun processAlert(alert: Alert): ProcessingResult {
        if (alert.trigger !is AlertTrigger.ReleaseTrigger) {
            return error("Invalid trigger type for release alert")
        }

        val trigger = alert.trigger as AlertTrigger.ReleaseTrigger
        val validationResult = validateAlertSpecific(trigger)
        if (!validationResult.first) {
            return notTriggered(validationResult.second)
        }

        return try {
            val releaseResult = checkForNewReleases(trigger)
            when (releaseResult) {
                is ReleaseResult.Success -> {
                    if (releaseResult.releases.isNotEmpty()) {
                        triggered(
                            message = buildReleaseMessage(trigger.creator, releaseResult.releases),
                            data = mapOf(
                                "creator" to trigger.creator,
                                "mediaType" to trigger.mediaType.name,
                                "releases" to releaseResult.releases.joinToString(",") { it.title }
                            ),
                            metadata = mapOf(
                                "releaseCount" to releaseResult.releases.size.toString(),
                                "latestRelease" to releaseResult.releases.first().releaseDate
                            )
                        )
                    } else {
                        notTriggered("No new releases found")
                    }
                }
                is ReleaseResult.Error -> {
                    error(releaseResult.message)
                }
            }
        } catch (e: Exception) {
            logError("Error processing release alert", e)
            error("Failed to process release: ${e.message}")
        }
    }

    override fun getConfigurationSchema(): Map<String, ConfigurationField> = mapOf(
        "creator" to ConfigurationField(
            type = ConfigurationFieldType.STRING,
            required = true,
            description = "Creator or artist to monitor for releases"
        ),
        "mediaType" to ConfigurationField(
            type = ConfigurationFieldType.ENUM,
            required = true,
            description = "Type of media to monitor",
            options = MediaType.values().map { it.name }
        ),
        "minRating" to ConfigurationField(
            type = ConfigurationFieldType.NUMBER,
            required = false,
            description = "Minimum rating threshold for releases",
            defaultValue = "0"
        )
    )

    protected fun validateAlertSpecific(trigger: AlertTrigger.ReleaseTrigger): Pair<Boolean, String> {
        if (trigger.creator.isBlank()) {
            logWarning("Creator is blank")
            return false to "Creator is required"
        }

        if (trigger.mediaType == MediaType.UNKNOWN) {
            logWarning("Invalid media type")
            return false to "Invalid media type"
        }

        if (trigger.minRating < 0 || trigger.minRating > 10) {
            logWarning("Invalid rating range")
            return false to "Rating must be between 0 and 10"
        }

        return true to ""
    }

    protected abstract suspend fun checkForNewReleases(trigger: AlertTrigger.ReleaseTrigger): ReleaseResult

    protected fun buildReleaseMessage(creator: String, releases: List<Release>): String {
        return if (releases.size == 1) {
            val release = releases.first()
            "New release from $creator: ${release.title} (${release.releaseDate})"
        } else {
            "New releases from $creator: ${releases.size} new items"
        }
    }
}

sealed class ReleaseResult {
    data class Success(val releases: List<Release>) : ReleaseResult()
    data class Error(val message: String) : ReleaseResult()
}

@Serializable
data class Release(
    val title: String,
    val releaseDate: String,
    val rating: Double,
    val url: String,
    val description: String? = null,
    val imageUrl: String? = null
)
