package com.example.alertapp.models.price

import kotlinx.serialization.Serializable

@Serializable
enum class AssetType {
    STOCK,
    CRYPTO,
    FOREX,
    COMMODITY,
    INDEX;

    fun getApiSymbol(): String = when (this) {
        STOCK -> "EQUITY"
        CRYPTO -> "DIGITAL_CURRENCY"
        FOREX -> "FX"
        COMMODITY -> "PHYSICAL_CURRENCY"
        INDEX -> "INDEX"
    }

    companion object {
        fun fromSymbol(symbol: String): AssetType {
            return when {
                symbol.contains("/") -> FOREX
                symbol.endsWith("USD") || symbol.endsWith("USDT") -> CRYPTO
                symbol.startsWith("^") -> INDEX
                symbol.contains("=F") -> COMMODITY
                else -> STOCK
            }
        }
    }
}
