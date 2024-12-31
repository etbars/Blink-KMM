package com.example.alertapp.android.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.example.alertapp.android.MainActivity
import com.example.alertapp.android.R
import com.example.alertapp.models.Alert
import com.example.alertapp.services.AlertProcessor
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class AlertWidgetProvider : AppWidgetProvider() {
    @Inject lateinit var alertProcessor: AlertProcessor
    
    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.Main + job)

    companion object {
        const val ACTION_TOGGLE_ALERT = "com.example.alertapp.ACTION_TOGGLE_ALERT"
        const val EXTRA_ALERT_ID = "alert_id"

        fun updateWidgets(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, AlertWidgetProvider::class.java))
            val intent = Intent(context, AlertWidgetProvider::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
            }
            context.sendBroadcast(intent)
        }
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        scope.launch {
            val alerts = alertProcessor.getAlerts().firstOrNull() ?: emptyList()
            appWidgetIds.forEach { widgetId ->
                updateWidget(context, appWidgetManager, widgetId, alerts)
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        
        if (intent.action == ACTION_TOGGLE_ALERT) {
            val alertId = intent.getStringExtra(EXTRA_ALERT_ID) ?: return
            scope.launch {
                val alert = alertProcessor.getAlerts()
                    .firstOrNull()
                    ?.find { it.id == alertId }
                    ?: return@launch
                
                alertProcessor.updateAlert(alert.copy(enabled = !alert.enabled))
                updateWidgets(context)
            }
        }
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        job.cancel()
    }

    private fun updateWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        alerts: List<Alert>
    ) {
        val remoteViews = RemoteViews(context.packageName, R.layout.widget_alerts)

        // Set up click listener for the widget
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        remoteViews.setOnClickPendingIntent(R.id.widget_container, pendingIntent)

        // Clear existing views
        remoteViews.removeAllViews(R.id.alerts_container)

        // Add alert items
        alerts.take(3).forEach { alert ->
            val alertView = RemoteViews(context.packageName, R.layout.widget_alert_item)
            alertView.setTextViewText(R.id.alert_name, alert.name)
            alertView.setImageViewResource(
                R.id.alert_icon,
                getAlertTypeIcon(alert)
            )
            alertView.setImageViewResource(
                R.id.alert_toggle,
                if (alert.enabled) R.drawable.ic_toggle_on else R.drawable.ic_toggle_off
            )

            // Set up toggle click listener
            val toggleIntent = Intent(context, AlertWidgetProvider::class.java).apply {
                action = ACTION_TOGGLE_ALERT
                putExtra(EXTRA_ALERT_ID, alert.id)
            }
            val togglePendingIntent = PendingIntent.getBroadcast(
                context,
                alert.id.hashCode(),
                toggleIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alertView.setOnClickPendingIntent(R.id.alert_toggle, togglePendingIntent)

            remoteViews.addView(R.id.alerts_container, alertView)
        }

        // Update the widget
        appWidgetManager.updateAppWidget(appWidgetId, remoteViews)
    }

    private fun getAlertTypeIcon(alert: Alert): Int {
        return when (alert.type) {
            AlertType.WEATHER -> R.drawable.ic_weather
            AlertType.PRICE -> R.drawable.ic_price
            AlertType.CONTENT -> R.drawable.ic_content
            AlertType.RELEASE -> R.drawable.ic_release
        }
    }
}
