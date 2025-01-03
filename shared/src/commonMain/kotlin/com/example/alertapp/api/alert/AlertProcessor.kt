package com.example.alertapp.api.alert

import com.example.alertapp.api.ApiResponse
import com.example.alertapp.api.errors.ApiError
import com.example.alertapp.api.content.ContentProvider
import com.example.alertapp.api.news.NewsApiProvider
import com.example.alertapp.api.price.PriceProvider
import com.example.alertapp.api.release.ReleaseProvider
import com.example.alertapp.api.weather.OpenWeatherProvider
import com.example.alertapp.models.Alert
import com.example.alertapp.models.AlertTrigger
import com.example.alertapp.models.AlertTrigger.PriceTrigger
import com.example.alertapp.models.AlertTrigger.ContentTrigger
import com.example.alertapp.models.AlertTrigger.ReleaseTrigger
import com.example.alertapp.models.AlertTrigger.WeatherTrigger
import com.example.alertapp.models.AlertTrigger.EventTrigger
import com.example.alertapp.models.AlertTrigger.CustomTrigger
import com.example.alertapp.models.ProcessingResult
import com.example.alertapp.enums.Operator
import com.example.alertapp.models.weather.WeatherCondition
import com.example.alertapp.models.weather.WeatherConditionType
import com.example.alertapp.models.weather.WeatherData
import com.example.alertapp.models.content.ContentFilter
import co.touchlab.kermit.Logger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.datetime.Clock
import kotlinx.serialization.Serializable

class AlertProcessor(
    private val priceProvider: PriceProvider,
    private val contentProvider: ContentProvider,
    private val releaseProvider: ReleaseProvider,
    private val weatherProvider: OpenWeatherProvider,
    private val logger: Logger = Logger.withTag("AlertProcessor")
) {
    suspend fun processAlert(alert: Alert): Flow<ProcessingResult> = flow {
        try {
            val validationResult = validateAlert(alert)
            if (validationResult != null) {
                emit(validationResult)
                return@flow
            }

            val result = when (val trigger = alert.trigger) {
                is PriceTrigger -> processPriceTrigger(trigger)
                is ContentTrigger -> processContentTrigger(trigger)
                is ReleaseTrigger -> processReleaseTrigger(trigger)
                is WeatherTrigger -> processWeatherTrigger(trigger)
                is EventTrigger -> ProcessingResult.NotTriggered(message = "Event triggers not yet implemented")
                is CustomTrigger -> ProcessingResult.NotTriggered(message = "Custom triggers not yet implemented")
            }

            emit(result)
        } catch (e: Exception) {
            logger.e("Failed to process alert", e)
            emit(ProcessingResult.Error(
                message = e.message ?: "Unknown error",
                code = "PROCESSING_ERROR"
            ))
        }
    }

    private fun validateAlert(alert: Alert): ProcessingResult? {
        return when (val trigger = alert.trigger) {
            is PriceTrigger -> validatePriceTrigger(trigger)
            is ContentTrigger -> validateContentTrigger(trigger)
            is ReleaseTrigger -> validateReleaseTrigger(trigger)
            is WeatherTrigger -> validateWeatherTrigger(trigger)
            is EventTrigger -> ProcessingResult.Error(message = "Event triggers not yet implemented", code = "NOT_IMPLEMENTED")
            is CustomTrigger -> ProcessingResult.Error(message = "Custom triggers not yet implemented", code = "NOT_IMPLEMENTED")
        }
    }

    private fun validatePriceTrigger(trigger: PriceTrigger): ProcessingResult? {
        if (trigger.asset.isBlank()) {
            return ProcessingResult.Error(message = "Asset cannot be empty", code = "INVALID_ASSET")
        }
        if (trigger.threshold <= 0) {
            return ProcessingResult.Error(message = "Threshold must be positive", code = "INVALID_THRESHOLD")
        }
        if (trigger.timeframe.isBlank()) {
            return ProcessingResult.Error(message = "Timeframe cannot be empty", code = "INVALID_TIMEFRAME")
        }
        return null
    }

    private fun validateContentTrigger(trigger: ContentTrigger): ProcessingResult? {
        if (trigger.query.isBlank()) {
            return ProcessingResult.Error(message = "Query cannot be empty", code = "INVALID_QUERY")
        }
        return null
    }

    private fun validateReleaseTrigger(trigger: ReleaseTrigger): ProcessingResult? {
        if (trigger.type.isBlank()) {
            return ProcessingResult.Error(message = "Release type cannot be empty", code = "INVALID_RELEASE_TYPE")
        }
        return null
    }

    private fun validateWeatherTrigger(trigger: WeatherTrigger): ProcessingResult? {
        if (trigger.conditions.isEmpty()) {
            return ProcessingResult.Error(message = "Weather conditions cannot be empty", code = "INVALID_CONDITIONS")
        }
        return null
    }

    private suspend fun processPriceTrigger(trigger: PriceTrigger): ProcessingResult {
        val response = priceProvider.getCurrentPrice(trigger.asset)
        return when (response) {
            is ApiResponse.Success -> {
                val price = response.data
                val triggered = when (trigger.operator) {
                    Operator.GREATER_THAN -> price > trigger.threshold
                    Operator.LESS_THAN -> price < trigger.threshold
                    Operator.EQUAL_TO -> price == trigger.threshold
                    Operator.GREATER_THAN_OR_EQUAL -> price >= trigger.threshold
                    Operator.LESS_THAN_OR_EQUAL -> price <= trigger.threshold
                    Operator.NOT_EQUAL_TO -> price != trigger.threshold
                }

                if (triggered) {
                    ProcessingResult.Triggered(
                        message = "Price alert triggered: ${trigger.asset} is ${trigger.operator} ${trigger.threshold}",
                        metadata = mapOf(
                            "asset" to trigger.asset,
                            "price" to price.toString(),
                            "threshold" to trigger.threshold.toString(),
                            "operator" to trigger.operator.toString()
                        )
                    )
                } else {
                    ProcessingResult.NotTriggered(
                        message = "Price condition not met: ${trigger.asset} is not ${trigger.operator} ${trigger.threshold}"
                    )
                }
            }
            is ApiResponse.Error -> ProcessingResult.Error(
                message = response.error.message,
                code = response.error.code ?: "UNKNOWN_ERROR"
            )
            is ApiResponse.Loading -> ProcessingResult.NotTriggered(message = "Loading price data")
        }
    }

    private suspend fun processContentTrigger(trigger: ContentTrigger): ProcessingResult {
        val filter = ContentFilter(
            query = trigger.query,
            keywords = trigger.keywords,
            excludeKeywords = trigger.excludeKeywords,
            source = trigger.sources.firstOrNull(),
            minEngagement = trigger.minRating,
            category = trigger.contentType
        )
        
        val response = contentProvider.getContent(filter)
        return when (response) {
            is ApiResponse.Success -> {
                val content = response.data
                if (content.isNotEmpty()) {
                    ProcessingResult.Triggered(
                        message = "Content alert triggered: Found ${content.size} matching items",
                        metadata = mapOf(
                            "query" to trigger.query,
                            "source" to (trigger.sources.firstOrNull() ?: "all"),
                            "count" to content.size.toString()
                        )
                    )
                } else {
                    ProcessingResult.NotTriggered(message = "No matching content found")
                }
            }
            is ApiResponse.Error -> ProcessingResult.Error(
                message = response.error.message,
                code = response.error.code ?: "UNKNOWN_ERROR"
            )
            is ApiResponse.Loading -> ProcessingResult.NotTriggered(message = "Processing content data")
        }
    }

    private suspend fun processReleaseTrigger(trigger: ReleaseTrigger): ProcessingResult {
        val response = releaseProvider.getReleases(trigger.type, trigger.creator)
        return when (response) {
            is ApiResponse.Success -> {
                val releases = response.data
                if (releases.isNotEmpty()) {
                    ProcessingResult.Triggered(
                        message = "Release alert triggered: Found ${releases.size} new releases",
                        metadata = mapOf(
                            "type" to trigger.type.toString(),
                            "creator" to (trigger.creator ?: "any"),
                            "count" to releases.size.toString()
                        )
                    )
                } else {
                    ProcessingResult.NotTriggered(
                        message = "No new releases found for type: ${trigger.type}"
                    )
                }
            }
            is ApiResponse.Error -> ProcessingResult.Error(
                message = response.error.message,
                code = response.error.code ?: "UNKNOWN_ERROR"
            )
            is ApiResponse.Loading -> ProcessingResult.NotTriggered(message = "Loading release data")
        }
    }

    private suspend fun processWeatherTrigger(trigger: WeatherTrigger): ProcessingResult {
        val response = weatherProvider.getCurrentWeather(
            lat = trigger.location.latitude,
            lon = trigger.location.longitude
        )
        
        return when (response) {
            is ApiResponse.Success -> {
                val weather = response.data as WeatherData.Current
                val conditionResults = trigger.conditions.map { condition ->
                    val actual = when (condition.type) {
                        WeatherConditionType.TEMPERATURE -> weather.temperature?.toDouble() ?: 0.0
                        WeatherConditionType.HUMIDITY -> weather.humidity?.toDouble() ?: 0.0
                        WeatherConditionType.PRESSURE -> weather.pressure?.toDouble() ?: 0.0
                        WeatherConditionType.WIND_SPEED -> weather.windSpeed?.toDouble() ?: 0.0
                        WeatherConditionType.PRECIPITATION -> weather.precipitation?.toDouble() ?: 0.0
                        WeatherConditionType.CLOUDINESS -> weather.cloudCover?.toDouble() ?: 0.0
                        WeatherConditionType.UV_INDEX -> 0.0 // UV index not supported
                        WeatherConditionType.AIR_QUALITY -> 0.0 // Air quality not supported
                        WeatherConditionType.CUSTOM -> 0.0
                    }

                    val threshold = condition.threshold?.toDouble() ?: 0.0
                    val triggered = when (condition.operator) {
                        Operator.GREATER_THAN -> actual?.let { it > threshold } ?: false
                        Operator.LESS_THAN -> actual?.let { it < threshold } ?: false
                        Operator.EQUAL_TO -> actual?.let { it == threshold } ?: false
                        Operator.NOT_EQUAL_TO -> actual?.let { it != threshold } ?: false
                        Operator.GREATER_THAN_OR_EQUAL -> actual?.let { it >= threshold } ?: false
                        Operator.LESS_THAN_OR_EQUAL -> actual?.let { it <= threshold } ?: false
                    }

                    Triple(condition, actual, triggered)
                }

                val allTriggered = conditionResults.all { it.third }
                if (allTriggered) {
                    ProcessingResult.Triggered(
                        message = "Weather alert triggered: All conditions met",
                        metadata = conditionResults.associate { (condition, actual, _) ->
                            condition.type.toString() to "Actual: $actual, Expected: ${condition.operator} ${condition.threshold}".trim()
                        }
                    )
                } else {
                    val failedConditions = conditionResults.filterNot { it.third }
                        .map { (condition, actual, _) ->
                            "${condition.type} is $actual (expected ${condition.operator} ${condition.threshold})".trim()
                        }
                    ProcessingResult.NotTriggered(
                        message = "Weather conditions not met: ${failedConditions.joinToString("; ")}".trim()
                    )
                }
            }
            is ApiResponse.Error -> ProcessingResult.Error(
                message = response.error.message,
                code = response.error.code ?: "UNKNOWN_ERROR"
            )
            is ApiResponse.Loading -> ProcessingResult.NotTriggered(message = "Loading weather data")
        }
    }
}
