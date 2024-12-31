package com.example.alertapp.services.processors

import com.example.alertapp.network.services.PriceApiService
import org.koin.core.component.inject
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AndroidPriceAlertProcessor : PriceAlertProcessor() {
    private val priceApiService: PriceApiService by inject()

    override suspend fun getPriceHistory(asset: String): PriceResult {
        val result = priceApiService.getPriceHistory(asset, 60) // Get 1 hour of data by default
        return when (result) {
            is ApiResponse.Success -> {
                PriceResult.Success(result.data.map { 
                    PricePoint(
                        price = it.price,
                        volume = it.volume,
                        timestamp = it.timestamp.epochSeconds
                    )
                })
            }
            is ApiResponse.Error -> {
                PriceResult.Error(result.message)
            }
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
