package com.example.alertapp.nlp

import com.example.alertapp.models.Alert
import com.example.alertapp.models.Priority
import com.example.alertapp.models.AlertActionType
import com.example.alertapp.models.AlertTrigger
import com.example.alertapp.models.AlertAction
import com.example.alertapp.enums.Operator
import com.example.alertapp.enums.Sentiment
import com.example.alertapp.enums.ReleaseType
import com.example.alertapp.models.weather.WeatherLocation
import com.example.alertapp.models.weather.WeatherCondition
import com.example.alertapp.models.weather.WeatherConditionType
import com.example.alertapp.models.content.ContentFilter
import com.example.alertapp.services.openai.OpenAIService
import co.touchlab.kermit.Logger
import kotlinx.serialization.json.*
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlinx.datetime.Clock

class NaturalLanguageAlertParser : KoinComponent {
    private val openAIService: OpenAIService by inject()
    private val logger = Logger.withTag("NaturalLanguageAlertParser")
    private val json = Json { ignoreUnknownKeys = true }

    sealed interface ParserAlertContext {
        data class Initial(
            val intent: String? = null,
            val entities: Map<String, String> = emptyMap(),
            val lastAction: String? = null,
            val parameters: Map<String, String> = emptyMap()
        ) : ParserAlertContext

        data class AwaitingInfo(
            val intent: String,
            val entities: Map<String, String>,
            val missingFields: List<String>
        ) : ParserAlertContext
    }

    sealed interface ParseResult {
        data class Success(
            val alert: Alert,
            val compoundAlerts: List<Alert> = listOf(),
            val context: ParserAlertContext
        ) : ParseResult

        data class NeedsMoreInfo(
            val context: ParserAlertContext,
            val suggestedPrompts: List<String>
        ) : ParseResult

        data class Error(
            val message: String,
            val details: String? = null,
            val suggestions: List<String> = listOf()
        ) : ParseResult
    }

    private fun ParserAlertContext.toAlertContext(): AlertContext {
        return when (this) {
            is ParserAlertContext.Initial -> AlertContext(
                timestamp = Clock.System.now(),
                userId = "user", // TODO: Get actual user ID
                lastInput = lastAction ?: "",
                patterns = emptyList(),
                metadata = entities + parameters
            )
            is ParserAlertContext.AwaitingInfo -> AlertContext(
                timestamp = Clock.System.now(),
                userId = "user", // TODO: Get actual user ID
                lastInput = intent,
                patterns = emptyList(),
                metadata = entities + mapOf("missingFields" to missingFields.joinToString(","))
            )
        }
    }

    private fun AlertContext.toParserContext(): ParserAlertContext {
        val missingFields = metadata["missingFields"]?.split(",") ?: emptyList()
        return if (missingFields.isNotEmpty()) {
            ParserAlertContext.AwaitingInfo(
                intent = lastInput,
                entities = metadata.filterKeys { it != "missingFields" },
                missingFields = missingFields
            )
        } else {
            ParserAlertContext.Initial(
                intent = null,
                entities = metadata,
                lastAction = lastInput,
                parameters = emptyMap()
            )
        }
    }

    suspend fun parseAlertRequest(
        input: String,
        previousContext: ParserAlertContext? = null
    ): ParseResult {
        try {
            logger.d { "Parsing alert request: $input" }
            
            // Convert ParserAlertContext to AlertContext for OpenAIService
            val response = openAIService.parseAlertRequest(input, previousContext?.toAlertContext())
            logger.d { "Raw OpenAI response: ${response.parameters}" }
            
            // Check if more information is needed
            if (response.needsMoreInfo) {
                logger.i { "More information needed from user" }
                return ParseResult.NeedsMoreInfo(
                    context = response.context.toParserContext(),
                    suggestedPrompts = response.suggestedPrompts
                )
            }

            // Parse the response parameters
            val params = try {
                Json.parseToJsonElement(response.parameters).jsonObject
            } catch (e: Exception) {
                logger.e(e) { "Failed to parse OpenAI response: ${response.parameters}" }
                return ParseResult.Error(
                    message = "Invalid response format",
                    details = "Could not parse the response parameters: ${e.message}",
                    suggestions = listOf(
                        "Please try rephrasing your request",
                        "Make sure to provide clear alert details"
                    )
                )
            }

            // Validate required fields
            val missingFields = validateRequiredFields(params)
            if (missingFields.isNotEmpty()) {
                logger.w { "Missing required fields: $missingFields" }
                return ParseResult.Error(
                    message = "Invalid alert configuration",
                    details = "Missing required fields: ${missingFields.joinToString()}",
                    suggestions = listOf(
                        "Please specify the type of alert you want to create",
                        "Make sure to provide all required information",
                        "Try rephrasing your request to be more specific"
                    )
                )
            }

            // Create the main alert
            val alert = try {
                createAlert(params)
            } catch (e: Exception) {
                logger.e(e) { "Failed to create alert from params: $params" }
                return ParseResult.Error(
                    message = "Failed to create alert",
                    details = e.message,
                    suggestions = listOf(
                        "Please try rephrasing your request",
                        "Make sure all required information is provided"
                    )
                )
            }

            // Handle compound alerts if present
            val compoundAlerts = response.compoundAlerts.mapNotNull { compoundJson ->
                try {
                    createAlert(Json.parseToJsonElement(compoundJson).jsonObject)
                } catch (e: Exception) {
                    logger.w(e) { "Failed to create compound alert: $compoundJson" }
                    null
                }
            }

            logger.i { "Successfully created alert with ${compoundAlerts.size} compound alerts" }
            return ParseResult.Success(
                alert = alert,
                compoundAlerts = compoundAlerts,
                context = response.context.toParserContext()
            )

        } catch (e: Exception) {
            logger.e(e) { "Failed to process alert request" }
            return ParseResult.Error(
                message = "Failed to process alert request",
                details = e.message,
                suggestions = listOf(
                    "Please try again with a simpler request",
                    "Make sure your request is clear and specific"
                )
            )
        }
    }

    private fun validateRequiredFields(params: JsonObject): List<String> {
        val missingFields = mutableListOf<String>()
        
        // Core fields
        if (!params.containsKey("type")) missingFields.add("type")
        if (!params.containsKey("name")) missingFields.add("name")
        if (!params.containsKey("description")) missingFields.add("description")
        if (!params.containsKey("trigger")) missingFields.add("trigger")
        
        // Validate trigger structure
        params["trigger"]?.jsonObject?.let { trigger ->
            if (!trigger.containsKey("type")) missingFields.add("trigger.type")
        }
        
        return missingFields
    }

    private fun createAlert(params: JsonObject): Alert {
        val triggerType = params["trigger"]?.jsonObject?.get("type")?.jsonPrimitive?.content ?: throw IllegalArgumentException("Missing trigger type")
        val trigger = parseTrigger(triggerType, params["trigger"]?.jsonObject?.mapValues { it.value.jsonPrimitive.content } ?: emptyMap())
        val actions = params["actions"]?.jsonArray?.mapNotNull { actionJson ->
            val action = actionJson.jsonObject
            val type = action["type"]?.jsonPrimitive?.content ?: return@mapNotNull null
            parseAction(type, action.mapValues { it.value.jsonPrimitive.content })
        } ?: emptyList()

        return Alert(
            name = params["name"]?.jsonPrimitive?.content ?: throw IllegalArgumentException("Missing name"),
            description = params["description"]?.jsonPrimitive?.content ?: throw IllegalArgumentException("Missing description"),
            trigger = trigger,
            actions = actions,
            priority = Priority.DEFAULT
        )
    }

    private fun parseTrigger(type: String, parameters: Map<String, String>): AlertTrigger {
        return when (type.lowercase()) {
            "price" -> AlertTrigger.PriceTrigger(
                asset = parameters["asset"] ?: throw IllegalArgumentException("Asset is required for price trigger"),
                operator = Operator.valueOf(parameters["operator"]?.uppercase() ?: "GREATER_THAN"),
                threshold = parameters["threshold"]?.toDoubleOrNull() ?: throw IllegalArgumentException("Threshold is required for price trigger"),
                timeframe = parameters["timeframe"] ?: "1h"
            )
            "content" -> AlertTrigger.ContentTrigger(
                query = parameters["query"] ?: throw IllegalArgumentException("Query is required for content trigger"),
                sources = parameters["sources"]?.split(",")?.map { it.trim() } ?: emptyList(),
                authors = parameters["authors"]?.split(",")?.map { it.trim() } ?: emptyList(),
                minRating = parameters["minRating"]?.toDoubleOrNull(),
                sentiment = parameters["sentiment"]?.let { Sentiment.valueOf(it.uppercase()) },
                contentType = parameters["contentType"],
                keywords = parameters["keywords"]?.split(",")?.map { it.trim() } ?: emptyList(),
                excludeKeywords = parameters["excludeKeywords"]?.split(",")?.map { it.trim() } ?: emptyList()
            )
            "release" -> AlertTrigger.ReleaseTrigger(
                type = parameters["type"] ?: throw IllegalArgumentException("Type is required for release trigger"),
                creator = parameters["creator"],
                minRating = parameters["minRating"]?.toDoubleOrNull(),
                releaseType = parameters["releaseType"]?.let { ReleaseType.valueOf(it.uppercase()) },
                conditions = parameters["conditions"]?.split(",")?.map { it.trim() } ?: emptyList()
            )
            "weather" -> AlertTrigger.WeatherTrigger(
                location = WeatherLocation(
                    latitude = parameters["latitude"]?.toDoubleOrNull() ?: throw IllegalArgumentException("Latitude is required for weather trigger"),
                    longitude = parameters["longitude"]?.toDoubleOrNull() ?: throw IllegalArgumentException("Longitude is required for weather trigger"),
                    name = parameters["locationName"]
                ),
                conditions = parameters["conditions"]?.split(",")?.map { condition ->
                    WeatherCondition(
                        type = WeatherConditionType.valueOf(condition.trim().uppercase()),
                        threshold = parameters["${condition.trim()}_threshold"]?.toDoubleOrNull() ?: 0.0,
                        operator = parameters["${condition.trim()}_operator"]?.let { Operator.valueOf(it.uppercase()) } ?: Operator.GREATER_THAN
                    )
                } ?: emptyList()
            )
            "event" -> AlertTrigger.EventTrigger(
                categories = parameters["categories"]?.split(",")?.map { it.trim() } ?: emptyList(),
                locations = parameters["locations"]?.split(",")?.map { it.trim() } ?: emptyList(),
                keywords = parameters["keywords"]?.split(",")?.map { it.trim() } ?: emptyList(),
                timeRange = null // TODO: Implement EventTimeRange parsing
            )
            "custom" -> AlertTrigger.CustomTrigger(
                description = parameters["description"] ?: throw IllegalArgumentException("Description is required for custom trigger"),
                parameters = parameters.filterKeys { it != "description" }
            )
            else -> throw IllegalArgumentException("Unknown trigger type: $type")
        }
    }

    private fun parseAction(type: String, parameters: Map<String, String>): AlertAction {
        return when (type.lowercase()) {
            "notification" -> AlertAction.NotificationAction(
                title = parameters["title"] ?: throw IllegalArgumentException("Title is required for notification action"),
                message = parameters["message"] ?: throw IllegalArgumentException("Message is required for notification action"),
                priority = parameters["priority"]?.let { Priority.valueOf(it.uppercase()) } ?: Priority.DEFAULT,
                type = AlertActionType.NOTIFICATION
            )
            "email" -> AlertAction.EmailAction(
                recipient = parameters["recipient"] ?: throw IllegalArgumentException("Recipient is required for email action"),
                subject = parameters["subject"] ?: throw IllegalArgumentException("Subject is required for email action"),
                body = parameters["body"] ?: throw IllegalArgumentException("Body is required for email action"),
                attachments = parameters["attachments"]?.split(",")?.map { it.trim() } ?: emptyList(),
                type = AlertActionType.EMAIL,
                config = parameters.filterKeys { it !in setOf("recipient", "subject", "body", "attachments") }
            )
            "webhook" -> AlertAction.WebhookAction(
                url = parameters["url"] ?: throw IllegalArgumentException("URL is required for webhook action"),
                method = parameters["method"] ?: "POST",
                headers = parameters.filterKeys { it.startsWith("header_") }
                    .mapKeys { it.key.removePrefix("header_") },
                body = parameters["body"],
                type = AlertActionType.WEBHOOK
            )
            "sms" -> AlertAction.SmsAction(
                phoneNumber = parameters["phoneNumber"] ?: throw IllegalArgumentException("Phone number is required for SMS action"),
                body = parameters["body"] ?: throw IllegalArgumentException("Body is required for SMS action"),
                type = AlertActionType.SMS
            )
            else -> throw IllegalArgumentException("Unknown action type: $type")
        }
    }
}
