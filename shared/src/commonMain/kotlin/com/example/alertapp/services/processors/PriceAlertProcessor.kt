package com.example.alertapp.services.processors

import com.example.alertapp.api.ApiResponse
import com.example.alertapp.models.price.PricePoint
import com.example.alertapp.models.price.PriceResult
import kotlinx.datetime.Instant

abstract class PriceAlertProcessor {
    abstract suspend fun getPriceHistory(asset: String): PriceResult

    abstract fun logWarning(message: String)
    abstract fun logError(message: String, error: Throwable? = null)
    abstract fun logInfo(message: String)
}
