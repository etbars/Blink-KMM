package com.example.alertapp.android.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.alertapp.models.Alert
import com.example.alertapp.services.AlertProcessor
import com.example.alertapp.android.deeplink.DeepLinkHandler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AlertDetailViewModel @Inject constructor(
    private val alertProcessor: AlertProcessor,
    private val deepLinkHandler: DeepLinkHandler
) : ViewModel() {
    private val _alert = MutableStateFlow<Alert?>(null)
    val alert: StateFlow<Alert?> = _alert.asStateFlow()

    fun loadAlert(alertId: String) {
        viewModelScope.launch {
            alertProcessor.getAlerts()
                .map { alerts -> alerts.find { it.id == alertId } }
                .collect { alert ->
                    _alert.value = alert
                }
        }
    }

    fun toggleAlert(enabled: Boolean) {
        viewModelScope.launch {
            _alert.value?.let { currentAlert ->
                val updatedAlert = currentAlert.copy(
                    enabled = enabled,
                    updatedAt = System.currentTimeMillis()
                )
                alertProcessor.updateAlert(updatedAlert)
            }
        }
    }

    fun deleteAlert() {
        viewModelScope.launch {
            _alert.value?.let { currentAlert ->
                alertProcessor.deleteAlert(currentAlert)
            }
        }
    }

    fun getShareText(alert: Alert): String {
        return deepLinkHandler.createShareText(alert)
    }
}
