package com.example.alertapp.services.processors

import com.example.alertapp.models.Alert
import com.example.alertapp.models.ProcessingResult

abstract class BaseAlertProcessor {
    abstract suspend fun processAlert(alert: Alert): ProcessingResult
    
    protected fun validateAlert(alert: Alert): Boolean {
        return alert.enabled && alert.trigger != null
    }
}
