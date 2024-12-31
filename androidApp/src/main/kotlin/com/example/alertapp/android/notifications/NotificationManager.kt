package com.example.alertapp.android.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.alertapp.android.MainActivity
import com.example.alertapp.models.Alert
import com.example.alertapp.models.AlertType
import com.example.alertapp.models.NotificationPriority
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AlertNotificationManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val notificationManager = NotificationManagerCompat.from(context)

    companion object {
        private const val CHANNEL_ID_HIGH = "alert_high_priority"
        private const val CHANNEL_ID_DEFAULT = "alert_default"
        private const val CHANNEL_ID_LOW = "alert_low_priority"
        
        private const val NOTIFICATION_GROUP_KEY = "com.example.alertapp.ALERTS"
        private const val REQUEST_CODE_OPEN = 0
        private const val REQUEST_CODE_DISMISS = 1
        private const val REQUEST_CODE_SNOOZE = 2
    }

    init {
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val highChannel = NotificationChannel(
                CHANNEL_ID_HIGH,
                "High Priority Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Critical alerts that require immediate attention"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 250, 250, 250)
            }

            val defaultChannel = NotificationChannel(
                CHANNEL_ID_DEFAULT,
                "Regular Alerts",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Standard alerts and updates"
            }

            val lowChannel = NotificationChannel(
                CHANNEL_ID_LOW,
                "Low Priority Alerts",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Information and background updates"
            }

            notificationManager.createNotificationChannels(
                listOf(highChannel, defaultChannel, lowChannel)
            )
        }
    }

    fun showAlertNotification(alert: Alert, message: String) {
        val channelId = getChannelId(alert)
        val priority = getNotificationPriority(alert)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("alertId", alert.id)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            REQUEST_CODE_OPEN,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val dismissIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_DISMISS,
            Intent(context, NotificationActionReceiver::class.java).apply {
                action = NotificationActionReceiver.ACTION_DISMISS
                putExtra("alertId", alert.id)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val snoozeIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_SNOOZE,
            Intent(context, NotificationActionReceiver::class.java).apply {
                action = NotificationActionReceiver.ACTION_SNOOZE
                putExtra("alertId", alert.id)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle(alert.name)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(priority)
            .setAutoCancel(true)
            .setGroup(NOTIFICATION_GROUP_KEY)
            .setContentIntent(pendingIntent)
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "Dismiss",
                dismissIntent
            )
            .addAction(
                android.R.drawable.ic_popup_sync,
                "Snooze",
                snoozeIntent
            )
            .build()

        notificationManager.notify(alert.id.hashCode(), notification)
        
        // Show summary notification for grouped notifications
        showGroupSummaryNotification()
    }

    private fun showGroupSummaryNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val summaryNotification = NotificationCompat.Builder(context, CHANNEL_ID_DEFAULT)
                .setSmallIcon(android.R.drawable.ic_popup_reminder)
                .setGroup(NOTIFICATION_GROUP_KEY)
                .setGroupSummary(true)
                .setAutoCancel(true)
                .build()

            notificationManager.notify(0, summaryNotification)
        }
    }

    private fun getChannelId(alert: Alert): String {
        return when (getPriority(alert)) {
            NotificationPriority.HIGH -> CHANNEL_ID_HIGH
            NotificationPriority.LOW -> CHANNEL_ID_LOW
            else -> CHANNEL_ID_DEFAULT
        }
    }

    private fun getNotificationPriority(alert: Alert): Int {
        return when (getPriority(alert)) {
            NotificationPriority.HIGH -> NotificationCompat.PRIORITY_HIGH
            NotificationPriority.LOW -> NotificationCompat.PRIORITY_LOW
            else -> NotificationCompat.PRIORITY_DEFAULT
        }
    }

    private fun getPriority(alert: Alert): NotificationPriority {
        // Determine priority based on alert type and conditions
        return when (alert.type) {
            AlertType.WEATHER -> NotificationPriority.HIGH // Weather alerts are typically high priority
            AlertType.PRICE -> {
                // Price alerts could vary based on threshold or conditions
                if (alert.trigger is Alert.PriceTrigger && 
                    (alert.trigger.condition == PriceOperator.ABOVE || 
                     alert.trigger.condition == PriceOperator.BELOW)) {
                    NotificationPriority.HIGH
                } else {
                    NotificationPriority.DEFAULT
                }
            }
            AlertType.CONTENT -> NotificationPriority.LOW // Content updates are typically low priority
            else -> NotificationPriority.DEFAULT
        }
    }

    fun cancelNotification(alertId: String) {
        notificationManager.cancel(alertId.hashCode())
    }

    fun cancelAllNotifications() {
        notificationManager.cancelAll()
    }
}
