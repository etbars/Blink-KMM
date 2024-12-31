package com.example.alertapp.android.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.alertapp.models.Alert
import com.example.alertapp.services.AlertProcessor
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val alertProcessor: AlertProcessor
) : ViewModel() {
    private val _alerts = MutableStateFlow<List<Alert>>(emptyList())
    val alerts: StateFlow<List<Alert>> = _alerts.asStateFlow()

    init {
        viewModelScope.launch {
            alertProcessor.getAlerts().collect { alertList ->
                _alerts.value = alertList
            }
        }
    }

    fun addAlert(alert: Alert) {
        viewModelScope.launch {
            alertProcessor.addAlert(alert)
        }
    }

    fun toggleAlert(alert: Alert, enabled: Boolean) {
        viewModelScope.launch {
            alertProcessor.updateAlert(alert.copy(enabled = enabled))
        }
    }

    fun deleteAlert(alert: Alert) {
        viewModelScope.launch {
            alertProcessor.deleteAlert(alert)
        }
    }
}
