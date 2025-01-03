package com.example.alertapp.api.price

import com.example.alertapp.api.ApiResponse
import com.example.alertapp.models.price.PriceData
import co.touchlab.kermit.Logger

interface PriceProvider {
    suspend fun getCurrentPrice(symbol: String): ApiResponse<Double>
    suspend fun getHistoricalPrices(symbol: String, interval: String, outputSize: String): ApiResponse<List<PriceData>>
    suspend fun getDailyPrices(symbol: String, outputSize: String): ApiResponse<List<PriceData>>
}

class DefaultPriceProvider(
    private val logger: Logger = Logger.withTag("PriceProvider")
) : PriceProvider {
    override suspend fun getCurrentPrice(symbol: String): ApiResponse<Double> {
        // TODO: Implement actual price fetching logic
        return ApiResponse.Success(0.0)
    }

    override suspend fun getHistoricalPrices(symbol: String, interval: String, outputSize: String): ApiResponse<List<PriceData>> {
        // TODO: Implement actual historical price fetching logic
        return ApiResponse.Success(emptyList())
    }

    override suspend fun getDailyPrices(symbol: String, outputSize: String): ApiResponse<List<PriceData>> {
        // TODO: Implement actual daily price fetching logic
        return ApiResponse.Success(emptyList())
    }
}
