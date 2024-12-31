package com.example.alertapp.android.scheduler

import android.content.Context
import androidx.work.*
import com.example.alertapp.android.data.AlertRepository
import com.example.alertapp.android.processors.AndroidAlertProcessor
import com.example.alertapp.models.Alert
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton
import java.util.concurrent.TimeUnit

@Singleton
class AlertScheduler @Inject constructor(
    private val context: Context,
    private val alertProcessor: AndroidAlertProcessor,
    private val alertRepository: AlertRepository
) {
    private val workManager = WorkManager.getInstance(context)

    companion object {
        private const val ALERT_WORK_NAME = "alert_check"
        private const val MIN_INTERVAL_MINUTES = 15L
        private const val KEY_ALERT_ID = "alert_id"
    }

    suspend fun scheduleAlert(alert: Alert) {
        // Save or update the alert in repository
        alertRepository.insertAlert(alert)

        val alertData = workDataOf(
            KEY_ALERT_ID to alert.id
        )

        // Calculate check interval (minimum 15 minutes)
        val intervalMinutes = maxOf(alert.checkInterval / 60000, MIN_INTERVAL_MINUTES)

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val workRequest = PeriodicWorkRequestBuilder<AlertWorker>(
            intervalMinutes,
            TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .setInputData(alertData)
            .addTag(alert.id)
            .build()

        workManager.enqueueUniquePeriodicWork(
            "${ALERT_WORK_NAME}_${alert.id}",
            ExistingPeriodicWorkPolicy.UPDATE,
            workRequest
        )
    }

    suspend fun cancelAlert(alertId: String) {
        workManager.cancelAllWorkByTag(alertId)
        // Set alert as inactive in repository
        alertRepository.setAlertActive(alertId, false)
    }

    fun getAlertStatus(alertId: String): Flow<AlertStatus> {
        return workManager.getWorkInfosByTagFlow(alertId)
            .map { workInfoList ->
                val workInfo = workInfoList.firstOrNull()
                when (workInfo?.state) {
                    WorkInfo.State.RUNNING -> AlertStatus.CHECKING
                    WorkInfo.State.ENQUEUED -> AlertStatus.SCHEDULED
                    WorkInfo.State.SUCCEEDED -> AlertStatus.LAST_CHECK_SUCCEEDED
                    WorkInfo.State.FAILED -> AlertStatus.LAST_CHECK_FAILED
                    WorkInfo.State.BLOCKED -> AlertStatus.WAITING_FOR_CONDITIONS
                    WorkInfo.State.CANCELLED -> AlertStatus.CANCELLED
                    null -> AlertStatus.NOT_SCHEDULED
                }
            }
    }

    suspend fun scheduleAllActiveAlerts() {
        alertRepository.getActiveAlerts().collect { alerts ->
            alerts.forEach { alert ->
                scheduleAlert(alert)
            }
        }
    }
}

enum class AlertStatus {
    NOT_SCHEDULED,
    SCHEDULED,
    CHECKING,
    LAST_CHECK_SUCCEEDED,
    LAST_CHECK_FAILED,
    WAITING_FOR_CONDITIONS,
    CANCELLED
}

class AlertWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    @Inject
    lateinit var alertProcessor: AndroidAlertProcessor

    override suspend fun doWork(): Result {
        val alertId = inputData.getString(AlertScheduler.KEY_ALERT_ID) ?: return Result.failure()

        return try {
            val triggered = alertProcessor.processAlert(alertId)
            if (triggered) {
                Result.success()
            } else {
                Result.retry()
            }
        } catch (e: Exception) {
            Result.failure()
        }
    }
}
