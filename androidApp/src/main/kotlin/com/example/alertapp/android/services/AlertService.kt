package com.example.alertapp.android.services

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.example.alertapp.android.AlertApplication.Companion.ALERT_CHANNEL_ID
import com.example.alertapp.android.MainActivity
import com.example.alertapp.android.R
import com.example.alertapp.services.AlertProcessor
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject

@AndroidEntryPoint
class AlertService : Service() {
    @Inject lateinit var alertProcessor: AlertProcessor

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(serviceJob + Dispatchers.Main)
    private var wakeLock: PowerManager.WakeLock? = null
    private var isServiceRunning = false

    companion object {
        private const val NOTIFICATION_ID = 1
        private const val WAKELOCK_TAG = "AlertService:WakeLock"

        fun startService(context: Context) {
            val startIntent = Intent(context, AlertService::class.java)
            context.startForegroundService(startIntent)
        }

        fun stopService(context: Context) {
            val stopIntent = Intent(context, AlertService::class.java)
            context.stopService(stopIntent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        acquireWakeLock()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, createNotification())
        
        if (!isServiceRunning) {
            isServiceRunning = true
            startAlertProcessing()
        }

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        isServiceRunning = false
        serviceJob.cancel()
        releaseWakeLock()
    }

    private fun startAlertProcessing() {
        serviceScope.launch {
            try {
                alertProcessor.startProcessing()
                    .catch { e ->
                        // Log error and restart processing
                        e.printStackTrace()
                        delay(5000) // Wait before retrying
                        startAlertProcessing()
                    }
                    .collect { processingResult ->
                        when (processingResult) {
                            is AlertProcessor.ProcessingResult.AlertTriggered -> {
                                showAlertNotification(processingResult.alert)
                            }
                            is AlertProcessor.ProcessingResult.Error -> {
                                // Handle error, maybe show error notification
                                processingResult.error.printStackTrace()
                            }
                        }
                    }
            } catch (e: Exception) {
                e.printStackTrace()
                // Restart processing on error
                delay(5000)
                startAlertProcessing()
            }
        }
    }

    private fun createNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, ALERT_CHANNEL_ID)
            .setContentTitle(getString(R.string.app_name))
            .setContentText("Monitoring alerts...")
            .setSmallIcon(R.drawable.ic_notification)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .build()
    }

    private fun showAlertNotification(alert: Alert) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, ALERT_CHANNEL_ID)
            .setContentTitle(alert.name)
            .setContentText(alert.description)
            .setSmallIcon(R.drawable.ic_notification)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(alert.id.hashCode(), notification)
    }

    private fun acquireWakeLock() {
        wakeLock = (getSystemService(Context.POWER_SERVICE) as PowerManager).run {
            newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, WAKELOCK_TAG).apply {
                acquire(10*60*1000L /*10 minutes*/)
            }
        }
    }

    private fun releaseWakeLock() {
        wakeLock?.let {
            if (it.isHeld) {
                it.release()
            }
        }
        wakeLock = null
    }
}
