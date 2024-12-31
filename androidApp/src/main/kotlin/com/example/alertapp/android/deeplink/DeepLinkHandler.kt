package com.example.alertapp.android.deeplink

import android.content.Intent
import android.net.Uri
import androidx.core.net.toUri
import com.example.alertapp.models.Alert
import com.example.alertapp.models.AlertType
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeepLinkHandler @Inject constructor() {
    companion object {
        const val SCHEME = "alertapp"
        const val HOST = "app.alertapp.example.com"
        const val PATH_ALERT = "alert"
        const val PATH_SHARE = "share"
        
        // Deep link format: alertapp://app.alertapp.example.com/alert/{alertId}
        // Web link format: https://app.alertapp.example.com/alert/{alertId}
    }

    fun handleIntent(intent: Intent): DeepLinkResult {
        val uri = intent.data ?: return DeepLinkResult.Error("No URI found")
        return parseUri(uri)
    }

    fun parseUri(uri: Uri): DeepLinkResult {
        return when {
            isValidScheme(uri) -> {
                when (uri.pathSegments.firstOrNull()) {
                    PATH_ALERT -> handleAlertPath(uri)
                    PATH_SHARE -> handleSharePath(uri)
                    else -> DeepLinkResult.Error("Invalid path")
                }
            }
            else -> DeepLinkResult.Error("Invalid scheme")
        }
    }

    private fun isValidScheme(uri: Uri): Boolean {
        return uri.scheme == SCHEME || uri.scheme == "https"
    }

    private fun handleAlertPath(uri: Uri): DeepLinkResult {
        val alertId = uri.pathSegments.getOrNull(1)
            ?: return DeepLinkResult.Error("Alert ID not found")
        
        return DeepLinkResult.ViewAlert(alertId)
    }

    private fun handleSharePath(uri: Uri): DeepLinkResult {
        try {
            val type = uri.getQueryParameter("type")?.let {
                AlertType.valueOf(it.uppercase())
            } ?: return DeepLinkResult.Error("Alert type not found")

            val name = uri.getQueryParameter("name")
                ?: return DeepLinkResult.Error("Alert name not found")

            val description = uri.getQueryParameter("description")
                ?: return DeepLinkResult.Error("Alert description not found")

            return DeepLinkResult.ShareAlert(
                name = name,
                description = description,
                type = type
            )
        } catch (e: Exception) {
            return DeepLinkResult.Error(e.message ?: "Invalid share parameters")
        }
    }

    fun createAlertDeepLink(alert: Alert): Uri {
        return "$SCHEME://$HOST/$PATH_ALERT/${alert.id}".toUri()
    }

    fun createShareDeepLink(alert: Alert): Uri {
        return "$SCHEME://$HOST/$PATH_SHARE".toUri().buildUpon()
            .appendQueryParameter("type", alert.type.name)
            .appendQueryParameter("name", alert.name)
            .appendQueryParameter("description", alert.description)
            .build()
    }

    fun createShareText(alert: Alert): String {
        val webLink = "https://$HOST/$PATH_ALERT/${alert.id}"
        return """
            Check out this alert from AlertApp:
            
            ${alert.name}
            ${alert.description}
            
            Open in AlertApp: $webLink
        """.trimIndent()
    }
}

sealed class DeepLinkResult {
    data class ViewAlert(val alertId: String) : DeepLinkResult()
    data class ShareAlert(
        val name: String,
        val description: String,
        val type: AlertType
    ) : DeepLinkResult()
    data class Error(val message: String) : DeepLinkResult()
}
