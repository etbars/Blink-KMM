package com.example.alertapp.android.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.alertapp.android.scheduler.AlertScheduler
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class NotificationActionReceiver : BroadcastReceiver() {
    @Inject
    lateinit var notificationManager: AlertNotificationManager
    
    @Inject
    lateinit var alertScheduler: AlertScheduler

    companion object {
        const val ACTION_DISMISS = "com.example.alertapp.DISMISS_ALERT"
        const val ACTION_SNOOZE = "com.example.alertapp.SNOOZE_ALERT"
        private const val SNOOZE_DURATION_MINUTES = 30L
    }

    override fun onReceive(context: Context, intent: Intent) {
        val alertId = intent.getStringExtra("alertId") ?: return

        when (intent.action) {
            ACTION_DISMISS -> {
                notificationManager.cancelNotification(alertId)
            }
            ACTION_SNOOZE -> {
                notificationManager.cancelNotification(alertId)
                // Re-schedule the alert with a delay
                CoroutineScope(Dispatchers.IO).launch {
                    // TODO: Implement snooze logic with AlertScheduler
                }
            }
        }
    }
}
