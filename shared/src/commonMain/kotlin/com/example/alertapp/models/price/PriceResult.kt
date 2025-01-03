package com.example.alertapp.models.price

sealed class PriceResult {
    data class Success(val data: List<PricePoint>) : PriceResult()
    data class Error(val message: String) : PriceResult()
    object Loading : PriceResult()
}
