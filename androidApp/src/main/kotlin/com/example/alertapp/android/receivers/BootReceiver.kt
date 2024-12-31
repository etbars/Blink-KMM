package com.example.alertapp.android.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.alertapp.android.services.AlertService
import com.example.alertapp.config.ConfigManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {
    @Inject lateinit var configManager: ConfigManager
    
    private val job = SupervisorJob()
    private val scope = CoroutineScope(job + Dispatchers.Main)

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            scope.launch {
                try {
                    // Initialize configurations
                    configManager.initialize()
                    
                    // Start the alert service
                    AlertService.startService(context)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }
}
