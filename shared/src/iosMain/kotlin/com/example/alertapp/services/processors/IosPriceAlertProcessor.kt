package com.example.alertapp.services.processors

import platform.Foundation.NSLog

class IosPriceAlertProcessor(
    private val priceService: PriceService
) : PriceAlertProcessor() {

    override suspend fun getPriceHistory(asset: String, timeframeMinutes: Int): PriceResult {
        return try {
            val priceData = priceService.getPriceHistory(asset, timeframeMinutes)
            PriceResult.Success(priceData.map { 
                PricePoint(it.timestamp, it.price, it.volume) 
            })
        } catch (e: Exception) {
            logError("Failed to fetch price data", e)
            PriceResult.Error("Failed to fetch price data: ${e.message}")
        }
    }

    override fun logWarning(message: String) {
        NSLog("⚠️ Price Alert: $message")
    }

    override fun logError(message: String, error: Throwable?) {
        if (error != null) {
            NSLog("❌ Price Alert Error: $message - ${error.message}")
        } else {
            NSLog("❌ Price Alert Error: $message")
        }
    }

    override fun logInfo(message: String) {
        NSLog("ℹ️ Price Alert: $message")
    }
}
