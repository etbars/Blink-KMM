package com.example.alertapp.android.processors

import com.example.alertapp.models.Alert
import com.example.alertapp.models.AlertType
import com.example.alertapp.models.ContentCondition
import com.example.alertapp.models.ContentData
import javax.inject.Inject
import javax.inject.Singleton
import java.util.regex.Pattern

@Singleton
class ContentAlertProcessor @Inject constructor(
    private val contentApi: ContentApi
) {
    suspend fun processAlert(alert: Alert): Boolean {
        if (alert.type != AlertType.CONTENT) return false

        val contentData = fetchContentData(alert.target ?: return false)
        return evaluateContentConditions(alert, contentData)
    }

    private suspend fun fetchContentData(url: String): ContentData {
        return contentApi.getContent(url)
    }

    private fun evaluateContentConditions(alert: Alert, contentData: ContentData): Boolean {
        val conditions = alert.conditions.mapNotNull { 
            ContentCondition.fromString(it)
        }

        return conditions.any { condition ->
            when (condition) {
                ContentCondition.KEYWORD_PRESENT -> {
                    val keywords = alert.keywords?.split(",")?.map { it.trim() } ?: return false
                    keywords.any { keyword ->
                        contentData.text.contains(keyword, ignoreCase = true)
                    }
                }
                ContentCondition.KEYWORD_ABSENT -> {
                    val keywords = alert.keywords?.split(",")?.map { it.trim() } ?: return false
                    keywords.none { keyword ->
                        contentData.text.contains(keyword, ignoreCase = true)
                    }
                }
                ContentCondition.PATTERN_MATCH -> {
                    val pattern = alert.pattern ?: return false
                    try {
                        Pattern.compile(pattern).matcher(contentData.text).find()
                    } catch (e: Exception) {
                        false
                    }
                }
                ContentCondition.CONTENT_CHANGED -> {
                    contentData.lastModified > (alert.lastChecked ?: 0)
                }
                ContentCondition.STATUS_CODE -> {
                    val expectedCode = alert.statusCode?.toIntOrNull() ?: return false
                    contentData.statusCode == expectedCode
                }
            }
        }
    }

    fun formatContentMessage(alert: Alert, contentData: ContentData): String {
        val conditions = alert.conditions.mapNotNull { ContentCondition.fromString(it) }
        val triggeredConditions = conditions.filter { condition ->
            when (condition) {
                ContentCondition.KEYWORD_PRESENT -> {
                    val keywords = alert.keywords?.split(",")?.map { it.trim() } ?: return@filter false
                    keywords.any { keyword ->
                        contentData.text.contains(keyword, ignoreCase = true)
                    }
                }
                ContentCondition.KEYWORD_ABSENT -> {
                    val keywords = alert.keywords?.split(",")?.map { it.trim() } ?: return@filter false
                    keywords.none { keyword ->
                        contentData.text.contains(keyword, ignoreCase = true)
                    }
                }
                ContentCondition.PATTERN_MATCH -> {
                    val pattern = alert.pattern ?: return@filter false
                    try {
                        Pattern.compile(pattern).matcher(contentData.text).find()
                    } catch (e: Exception) {
                        false
                    }
                }
                ContentCondition.CONTENT_CHANGED -> {
                    contentData.lastModified > (alert.lastChecked ?: 0)
                }
                ContentCondition.STATUS_CODE -> {
                    val expectedCode = alert.statusCode?.toIntOrNull() ?: return@filter false
                    contentData.statusCode == expectedCode
                }
            }
        }

        return buildString {
            append(alert.name)
            append("\n\nContent update for ${alert.target}:\n")
            append("Status Code: ${contentData.statusCode}\n")
            append("Last Modified: ${formatTimestamp(contentData.lastModified)}\n")
            append("Content Length: ${contentData.text.length} characters\n")
            
            if (triggeredConditions.contains(ContentCondition.KEYWORD_PRESENT)) {
                append("\nFound keywords:\n")
                alert.keywords?.split(",")?.map { it.trim() }?.forEach { keyword ->
                    if (contentData.text.contains(keyword, ignoreCase = true)) {
                        append("- $keyword\n")
                    }
                }
            }
            
            append("\nTriggered conditions:\n")
            triggeredConditions.forEach { condition ->
                append("- $condition\n")
            }
        }
    }

    private fun formatTimestamp(timestamp: Long): String {
        val date = java.util.Date(timestamp)
        return java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
            .format(date)
    }
}
