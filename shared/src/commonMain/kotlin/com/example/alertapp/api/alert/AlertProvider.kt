package com.example.alertapp.api.alert

import com.example.alertapp.api.ApiResponse
import com.example.alertapp.api.BaseApiProvider
import com.example.alertapp.api.errors.ApiError
import com.example.alertapp.models.Alert
import com.example.alertapp.models.AlertTrigger
import com.example.alertapp.models.AlertTrigger.PriceTrigger
import com.example.alertapp.models.AlertTrigger.ContentTrigger
import com.example.alertapp.models.AlertTrigger.ReleaseTrigger
import com.example.alertapp.models.AlertTrigger.WeatherTrigger
import com.example.alertapp.models.AlertTrigger.EventTrigger
import com.example.alertapp.models.AlertTrigger.CustomTrigger
import com.example.alertapp.models.weather.WeatherLocation
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.serialization.json.*
import co.touchlab.kermit.Logger

interface AlertProvider {
    suspend fun getAlerts(): ApiResponse<List<Alert>>
    suspend fun getAlert(id: String): ApiResponse<Alert>
    suspend fun createAlert(alert: Alert): ApiResponse<Alert>
    suspend fun updateAlert(alert: Alert): ApiResponse<Alert>
    suspend fun deleteAlert(id: String): ApiResponse<Unit>
    suspend fun triggerAlert(alert: Alert): ApiResponse<Unit>
}

class DefaultAlertProvider(
    private val httpClient: HttpClient,
    private val json: Json,
    private val logger: Logger = Logger.withTag("AlertProvider"),
    private val baseUrl: String = "http://localhost:8080"
) : AlertProvider {
    override suspend fun getAlerts(): ApiResponse<List<Alert>> {
        return try {
            val response = httpClient.get("$baseUrl/alerts")
            val body: String = response.body()
            ApiResponse.Success(json.decodeFromString(body))
        } catch (e: Exception) {
            logger.e("Failed to get alerts", e)
            ApiResponse.Error(ApiError.ServerError("Failed to get alerts"))
        }
    }

    override suspend fun getAlert(id: String): ApiResponse<Alert> {
        return try {
            val response = httpClient.get("$baseUrl/alerts/$id")
            val body: String = response.body()
            val alert = json.decodeFromString<Alert>(body)
            ApiResponse.Success(alert)
        } catch (e: Exception) {
            logger.e("Failed to get alert", e)
            ApiResponse.Error(ApiError.NotFoundError("Alert not found"))
        }
    }

    override suspend fun createAlert(alert: Alert): ApiResponse<Alert> {
        return try {
            if (!validateAlert(alert)) {
                return ApiResponse.Error(ApiError.ValidationError("Invalid alert data"))
            }
            val response = httpClient.post("$baseUrl/alerts") {
                contentType(ContentType.Application.Json)
                setBody(json.encodeToString(Alert.serializer(), alert))
            }
            val body: String = response.body()
            ApiResponse.Success(json.decodeFromString(body))
        } catch (e: Exception) {
            logger.e("Failed to create alert", e)
            ApiResponse.Error(ApiError.ServerError("Failed to create alert"))
        }
    }

    override suspend fun updateAlert(alert: Alert): ApiResponse<Alert> {
        return try {
            if (!validateAlert(alert)) {
                return ApiResponse.Error(ApiError.ValidationError("Invalid alert data"))
            }
            val response = httpClient.put("$baseUrl/alerts/${alert.id}") {
                contentType(ContentType.Application.Json)
                setBody(json.encodeToString(Alert.serializer(), alert))
            }
            val body: String = response.body()
            ApiResponse.Success(json.decodeFromString(body))
        } catch (e: Exception) {
            logger.e("Failed to update alert", e)
            ApiResponse.Error(ApiError.ServerError("Failed to update alert"))
        }
    }

    override suspend fun deleteAlert(id: String): ApiResponse<Unit> {
        return try {
            val response = httpClient.delete("$baseUrl/alerts/$id")
            ApiResponse.Success(Unit)
        } catch (e: Exception) {
            logger.e("Failed to delete alert", e)
            ApiResponse.Error(ApiError.ServerError("Failed to delete alert"))
        }
    }

    override suspend fun triggerAlert(alert: Alert): ApiResponse<Unit> {
        return try {
            val response = httpClient.post("$baseUrl/alerts/${alert.id}/trigger")
            ApiResponse.Success(Unit)
        } catch (e: Exception) {
            logger.e("Failed to trigger alert", e)
            ApiResponse.Error(ApiError.ServerError("Failed to trigger alert"))
        }
    }

    private fun validateAlert(alert: Alert): Boolean {
        return alert.id.isNotBlank() && alert.name.isNotBlank() && validateTrigger(alert.trigger)
    }

    private fun validateTrigger(trigger: AlertTrigger): Boolean {
        return when (trigger) {
            is AlertTrigger.PriceTrigger -> trigger.asset.isNotBlank() && trigger.threshold > 0
            is AlertTrigger.ContentTrigger -> trigger.query.isNotBlank()
            is AlertTrigger.ReleaseTrigger -> trigger.type.isNotBlank()
            is AlertTrigger.WeatherTrigger -> trigger.location.latitude in -90.0..90.0 && 
                                trigger.location.longitude in -180.0..180.0 && 
                                trigger.conditions.isNotEmpty()
            is AlertTrigger.EventTrigger -> trigger.categories.isNotEmpty() || trigger.locations.isNotEmpty() || trigger.keywords.isNotEmpty()
            is AlertTrigger.CustomTrigger -> trigger.description.isNotBlank() && trigger.parameters.isNotEmpty()
        }
    }
}
