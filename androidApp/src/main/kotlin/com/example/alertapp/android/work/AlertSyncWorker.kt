package com.example.alertapp.android.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.alertapp.services.AlertProcessor
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class AlertSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val alertProcessor: AlertProcessor
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            // Process all alerts
            alertProcessor.processAllAlerts()
            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            // Retry on failure
            Result.retry()
        }
    }
}
