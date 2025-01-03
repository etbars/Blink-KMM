package com.example.alertapp.services.processors

import com.example.alertapp.api.ApiResponse
import com.example.alertapp.models.price.PricePoint
import com.example.alertapp.models.price.PriceResult
import com.example.alertapp.network.services.PriceApiService
import com.example.alertapp.network.services.PriceDataPoint
import javax.inject.Inject
import javax.inject.Singleton
import timber.log.Timber
import kotlinx.datetime.Clock

@Singleton
class AndroidPriceAlertProcessor @Inject constructor(
    private val priceApiService: PriceApiService
) : PriceAlertProcessor() {

    override suspend fun getPriceHistory(asset: String): PriceResult {
        return try {
            when (val response = priceApiService.getPriceHistory(asset, 60)) {
                is ApiResponse.Success -> {
                    val points = response.data.map { dataPoint ->
                        PricePoint(
                            timestamp = dataPoint.timestamp,
                            open = dataPoint.openPrice ?: dataPoint.price,
                            high = dataPoint.highPrice ?: dataPoint.price,
                            low = dataPoint.lowPrice ?: dataPoint.price,
                            close = dataPoint.price,
                            volume = dataPoint.volume?.toLong() ?: 0L
                        )
                    }
                    PriceResult.Success(points)
                }
                is ApiResponse.Loading -> PriceResult.Error("Loading in progress")
                is ApiResponse.Error -> PriceResult.Error("Failed to get price history: ${response.error.message}")
            }
        } catch (e: Exception) {
            logError("Error getting price history", e)
            PriceResult.Error(e.message ?: "Unknown error")
        }
    }

    override fun logWarning(message: String) {
        Timber.w(message)
    }

    override fun logError(message: String, error: Throwable?) {
        if (error != null) {
            Timber.e(error, message)
        } else {
            Timber.e(message)
        }
    }

    override fun logInfo(message: String) {
        Timber.i(message)
    }
}
