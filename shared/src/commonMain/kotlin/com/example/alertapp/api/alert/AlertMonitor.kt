package com.example.alertapp.api.alert

import com.example.alertapp.models.Alert
import com.example.alertapp.models.ProcessingResult
import com.example.alertapp.repository.AlertRepository
import co.touchlab.kermit.Logger
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

class AlertMonitor(
    private val alertProcessor: AlertProcessor,
    private val alertRepository: AlertRepository,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
) {
    private val logger = Logger.withTag("AlertMonitor")
    private val _monitoringState = MutableStateFlow<MonitoringState>(MonitoringState.Idle)
    val monitoringState: StateFlow<MonitoringState> = _monitoringState.asStateFlow()

    private var monitoringJob: Job? = null

    fun startMonitoring() {
        if (monitoringJob?.isActive == true) return

        monitoringJob = scope.launch {
            _monitoringState.value = MonitoringState.Monitoring
            
            try {
                alertRepository.getEnabledAlerts()
                    .collect { alerts ->
                        processAlerts(alerts)
                        delay(60_000) // Check every minute
                    }
            } catch (e: Exception) {
                logger.e("Error during alert monitoring", e)
                _monitoringState.value = MonitoringState.Error(e)
            }
        }
    }

    fun stopMonitoring() {
        monitoringJob?.cancel()
        monitoringJob = null
        _monitoringState.value = MonitoringState.Idle
    }

    private suspend fun processAlerts(alerts: List<Alert>) {
        alerts.forEach { alert ->
            alertProcessor.processAlert(alert)
                .catch { e ->
                    logger.e("Error processing alert ${alert.id}", e)
                }
                .collect { result ->
                    when (result) {
                        is ProcessingResult.Triggered -> {
                            logger.i("Alert triggered: ${alert.id}")
                            _monitoringState.value = MonitoringState.AlertTriggered(alert, result)
                            alertRepository.updateLastTriggered(alert.id)
                        }
                        is ProcessingResult.Error -> {
                            logger.e("Error processing alert ${alert.id}: ${result.message}")
                        }
                        is ProcessingResult.NotTriggered -> {
                            logger.d("Alert not triggered: ${alert.id}")
                        }
                        else -> {} // Handle other cases if needed
                    }
                }
        }
    }
}

sealed class MonitoringState {
    object Idle : MonitoringState()
    object Monitoring : MonitoringState()
    data class AlertTriggered(val alert: Alert, val result: ProcessingResult.Triggered) : MonitoringState()
    data class Error(val error: Throwable) : MonitoringState()
}
