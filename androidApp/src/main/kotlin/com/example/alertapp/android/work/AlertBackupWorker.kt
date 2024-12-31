package com.example.alertapp.android.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.alertapp.config.backup.ConfigBackupManager
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class AlertBackupWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val backupManager: ConfigBackupManager
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            backupManager.createBackup()
            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }
}
