package com.example.alertapp.config

import kotlinx.serialization.Serializable

/**
 * Configuration for background work and tasks.
 */
@Serializable
data class WorkConfig(
    val workNames: WorkNames = WorkNames(),
    val intentActions: IntentActions = IntentActions(),
    val intentExtras: IntentExtras = IntentExtras()
)

/**
 * Names for background work tasks.
 */
@Serializable
data class WorkNames(
    val alertCheckWork: String = "alert_check_work",
    val syncWork: String = "sync_work"
)

/**
 * Intent actions for platform-specific implementations.
 */
@Serializable
data class IntentActions(
    val alertTriggered: String = "com.example.alertapp.ALERT_TRIGGERED",
    val alertDismissed: String = "com.example.alertapp.ALERT_DISMISSED"
)

/**
 * Intent extras for platform-specific implementations.
 */
@Serializable
data class IntentExtras(
    val alertId: String = "alert_id",
    val alertType: String = "alert_type",
    val notificationId: String = "notification_id"
)
