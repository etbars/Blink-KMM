package com.example.alertapp.android.processors

import com.example.alertapp.android.api.ApiResult
import com.example.alertapp.android.api.PriceApiImpl
import com.example.alertapp.models.Alert
import com.example.alertapp.models.AlertType
import com.example.alertapp.models.PriceCondition
import com.example.alertapp.models.PriceData
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PriceAlertProcessor @Inject constructor(
    private val priceApi: PriceApiImpl
) {
    suspend fun processAlert(alert: Alert): Boolean {
        if (alert.type != AlertType.PRICE) return false

        val symbol = alert.target ?: return false
        return when (val result = priceApi.getCurrentPrice(symbol)) {
            is ApiResult.Success -> {
                evaluatePriceConditions(alert, result.data)
            }
            is ApiResult.Error -> {
                // Log the error or notify the user if needed
                false
            }
        }
    }

    private fun evaluatePriceConditions(alert: Alert, priceData: PriceData): Boolean {
        val conditions = alert.conditions.mapNotNull { 
            PriceCondition.fromString(it)
        }

        return conditions.any { condition ->
            when (condition) {
                PriceCondition.PRICE_ABOVE -> {
                    val threshold = alert.threshold?.toDoubleOrNull() ?: return false
                    priceData.currentPrice > threshold
                }
                PriceCondition.PRICE_BELOW -> {
                    val threshold = alert.threshold?.toDoubleOrNull() ?: return false
                    priceData.currentPrice < threshold
                }
                PriceCondition.PERCENT_CHANGE_UP -> {
                    val threshold = alert.threshold?.toDoubleOrNull() ?: return false
                    val percentChange = ((priceData.currentPrice - priceData.openPrice) / priceData.openPrice) * 100
                    percentChange > threshold
                }
                PriceCondition.PERCENT_CHANGE_DOWN -> {
                    val threshold = alert.threshold?.toDoubleOrNull() ?: return false
                    val percentChange = ((priceData.currentPrice - priceData.openPrice) / priceData.openPrice) * 100
                    percentChange < -threshold
                }
                PriceCondition.VOLUME_SPIKE -> {
                    priceData.volume > priceData.averageVolume * 2
                }
            }
        }
    }

    suspend fun formatPriceMessage(alert: Alert): String? {
        val symbol = alert.target ?: return null
        return when (val result = priceApi.getCurrentPrice(symbol)) {
            is ApiResult.Success -> {
                val priceData = result.data
                val percentChange = ((priceData.currentPrice - priceData.openPrice) / priceData.openPrice) * 100
                val conditions = alert.conditions.mapNotNull { PriceCondition.fromString(it) }
                val triggeredConditions = conditions.filter { condition ->
                    when (condition) {
                        PriceCondition.PRICE_ABOVE -> {
                            val threshold = alert.threshold?.toDoubleOrNull() ?: return@filter false
                            priceData.currentPrice > threshold
                        }
                        PriceCondition.PRICE_BELOW -> {
                            val threshold = alert.threshold?.toDoubleOrNull() ?: return@filter false
                            priceData.currentPrice < threshold
                        }
                        PriceCondition.PERCENT_CHANGE_UP -> {
                            val threshold = alert.threshold?.toDoubleOrNull() ?: return@filter false
                            percentChange > threshold
                        }
                        PriceCondition.PERCENT_CHANGE_DOWN -> {
                            val threshold = alert.threshold?.toDoubleOrNull() ?: return@filter false
                            percentChange < -threshold
                        }
                        PriceCondition.VOLUME_SPIKE -> {
                            priceData.volume > priceData.averageVolume * 2
                        }
                    }
                }

                buildString {
                    append(alert.name)
                    append("\n\nCurrent price data for ${alert.target}:\n")
                    append("Current Price: $${String.format("%.2f", priceData.currentPrice)}\n")
                    append("Open Price: $${String.format("%.2f", priceData.openPrice)}\n")
                    append("Change: ${String.format("%.2f", percentChange)}%\n")
                    append("Volume: ${priceData.volume}\n")
                    if (priceData.averageVolume > 0) {
                        append("Avg Volume: ${priceData.averageVolume}\n")
                    }
                    append("\nTriggered conditions:\n")
                    triggeredConditions.forEach { condition ->
                        append("- $condition\n")
                    }
                }
            }
            is ApiResult.Error -> {
                "Error fetching price data: ${result.message}"
            }
        }
    }
}
