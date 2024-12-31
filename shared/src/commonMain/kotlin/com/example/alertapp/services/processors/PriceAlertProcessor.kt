package com.example.alertapp.services.processors

import com.example.alertapp.models.*
import com.example.alertapp.network.ApiResponse
import com.example.alertapp.services.base.AlertProcessor
import com.example.alertapp.services.base.ProcessingResultData
import kotlinx.serialization.Serializable

abstract class PriceAlertProcessor : AlertProcessor {

    override val supportedType: AlertType = AlertType.PRICE

    override suspend fun processAlert(alert: Alert): ApiResponse<ProcessingResultData> {
        if (alert.trigger !is AlertTrigger.PriceTrigger) {
            return ApiResponse.Error(message = "Invalid trigger type for price alert")
        }

        val trigger = alert.trigger as AlertTrigger.PriceTrigger
        
        // Get price data
        return when (val priceResult = getPriceHistory(trigger.asset)) {
            is PriceResult.Success -> {
                evaluatePrice(trigger, priceResult.data)
            }
            is PriceResult.Error -> {
                ApiResponse.Error(message = priceResult.message)
            }
        }
    }

    private fun evaluatePrice(trigger: AlertTrigger.PriceTrigger, priceData: List<PricePoint>): ApiResponse<ProcessingResultData> {
        if (priceData.isEmpty()) {
            return ApiResponse.Success(
                ProcessingResultData(
                    triggered = false,
                    reason = "No price data available"
                )
            )
        }

        val currentPrice = priceData.last().price
        val isTriggered = trigger.conditions.all { condition ->
            when (condition.field.lowercase()) {
                "price" -> evaluateNumericCondition(condition, currentPrice)
                "change_percent" -> {
                    val startPrice = priceData.first().price
                    val percentageChange = ((currentPrice - startPrice) / startPrice) * 100
                    evaluateNumericCondition(condition, percentageChange)
                }
                "volume" -> priceData.last().volume?.let { evaluateNumericCondition(condition, it) } ?: false
                else -> false
            }
        }

        return ApiResponse.Success(
            ProcessingResultData(
                triggered = isTriggered,
                reason = if (isTriggered) {
                    "Price conditions met for ${trigger.asset}: Current price is $${String.format("%.2f", currentPrice)}"
                } else {
                    "Price conditions not met for ${trigger.asset}: Current price is $${String.format("%.2f", currentPrice)}"
                },
                data = mapOf(
                    "asset" to trigger.asset,
                    "currentPrice" to currentPrice.toString(),
                    "startPrice" to priceData.first().price.toString(),
                    "volume" to (priceData.last().volume?.toString() ?: "N/A"),
                    "timestamp" to priceData.last().timestamp.toString()
                )
            )
        )
    }

    private fun evaluateNumericCondition(condition: AlertCondition, value: Double): Boolean {
        val threshold = condition.value.toDoubleOrNull() ?: return false
        
        return when (condition.operator) {
            AlertOperator.EQUALS -> value == threshold
            AlertOperator.NOT_EQUALS -> value != threshold
            AlertOperator.GREATER_THAN -> value > threshold
            AlertOperator.LESS_THAN -> value < threshold
            AlertOperator.GREATER_THAN_OR_EQUALS -> value >= threshold
            AlertOperator.LESS_THAN_OR_EQUALS -> value <= threshold
            else -> false
        }
    }

    protected abstract suspend fun getPriceHistory(asset: String): PriceResult
}

@Serializable
sealed class PriceResult {
    @Serializable
    data class Success(val data: List<PricePoint>) : PriceResult()

    @Serializable
    data class Error(val message: String) : PriceResult()
}

@Serializable
data class PricePoint(
    val price: Double,
    val volume: Double? = null,
    val timestamp: Long
)
