package com.example.alertapp.nlp

import com.example.alertapp.models.*
import com.example.alertapp.services.openai.OpenAIService
import kotlinx.serialization.json.*
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import co.touchlab.kermit.Logger

class NaturalLanguageAlertParser : KoinComponent {
    private val openAIService: OpenAIService by inject()
    private val json = Json { ignoreUnknownKeys = true }
    private val logger = Logger.withTag("NaturalLanguageAlertParser")

    sealed class ParseResult {
        data class Success(
            val alert: Alert,
            val compoundAlerts: List<Alert> = listOf(),
            val context: AlertContext
        ) : ParseResult()

        data class NeedsMoreInfo(
            val context: AlertContext,
            val suggestedPrompts: List<String>
        ) : ParseResult()

        data class Error(
            val message: String,
            val details: String? = null,
            val suggestions: List<String> = listOf()
        ) : ParseResult()
    }

    suspend fun parseAlertRequest(
        input: String,
        previousContext: AlertContext? = null
    ): ParseResult {
        try {
            logger.d { "Parsing alert request: $input" }
            
            // Get response from OpenAI
            val response = openAIService.parseAlertRequest(input, previousContext)
            logger.d { "Raw OpenAI response: ${response.parameters}" }
            
            // Check if more information is needed
            if (response.needsMoreInfo) {
                logger.i { "More information needed from user" }
                return ParseResult.NeedsMoreInfo(
                    context = response.context,
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
                context = response.context
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
            if (!trigger.containsKey("conditions")) missingFields.add("trigger.conditions")
            if (!trigger.containsKey("frequency")) missingFields.add("trigger.frequency")
        }
        
        return missingFields
    }

    private fun createAlert(params: JsonObject): Alert {
        val trigger = params["trigger"]?.jsonObject ?: throw IllegalArgumentException("Missing trigger")
        val conditions = trigger["conditions"]?.jsonArray ?: throw IllegalArgumentException("Missing conditions")
        val sources = params["sources"]?.jsonArray
        val actions = params["actions"]?.jsonArray
        val metadata = params["metadata"]?.jsonObject

        return Alert(
            type = params["type"]?.jsonPrimitive?.content ?: throw IllegalArgumentException("Missing type"),
            name = params["name"]?.jsonPrimitive?.content ?: throw IllegalArgumentException("Missing name"),
            description = params["description"]?.jsonPrimitive?.content ?: throw IllegalArgumentException("Missing description"),
            trigger = AlertTrigger(
                conditions = parseConditions(conditions),
                frequency = trigger["frequency"]?.jsonPrimitive?.content ?: throw IllegalArgumentException("Missing frequency"),
                schedule = trigger["schedule"]?.jsonObject?.let { parseSchedule(it) },
                logic = trigger["logic"]?.jsonPrimitive?.content ?: "OR"
            ),
            sources = sources?.let { parseSources(it) } ?: emptyList(),
            actions = actions?.let { parseActions(it) } ?: emptyList(),
            metadata = metadata?.let { parseMetadata(it) } ?: emptyMap()
        )
    }

    private fun parseConditions(conditions: JsonArray): List<AlertCondition> {
        return conditions.map { condition ->
            val conditionObj = condition.jsonObject
            AlertCondition(
                type = conditionObj["type"]?.jsonPrimitive?.content ?: throw IllegalArgumentException("Missing condition type"),
                operator = conditionObj["operator"]?.jsonPrimitive?.content ?: throw IllegalArgumentException("Missing operator"),
                value = conditionObj["value"]?.jsonPrimitive?.content ?: throw IllegalArgumentException("Missing value"),
                metadata = conditionObj["metadata"]?.jsonObject?.let { parseMetadata(it) } ?: emptyMap()
            )
        }
    }

    private fun parseSchedule(schedule: JsonObject): AlertSchedule {
        return AlertSchedule(
            type = schedule["type"]?.jsonPrimitive?.content ?: throw IllegalArgumentException("Missing schedule type"),
            pattern = schedule["pattern"]?.jsonPrimitive?.content ?: throw IllegalArgumentException("Missing pattern"),
            timezone = schedule["timezone"]?.jsonPrimitive?.content ?: "UTC"
        )
    }

    private fun parseSources(sources: JsonArray): List<AlertSource> {
        return sources.map { source ->
            val sourceObj = source.jsonObject
            AlertSource(
                type = sourceObj["type"]?.jsonPrimitive?.content ?: throw IllegalArgumentException("Missing source type"),
                url = sourceObj["url"]?.jsonPrimitive?.content ?: throw IllegalArgumentException("Missing URL"),
                parameters = sourceObj["parameters"]?.jsonObject?.let { parseMetadata(it) } ?: emptyMap(),
                auth = sourceObj["auth"]?.jsonObject?.let { parseMetadata(it) }
            )
        }
    }

    private fun parseActions(actions: JsonArray): List<AlertAction> {
        return actions.map { action ->
            val actionObj = action.jsonObject
            AlertAction(
                type = actionObj["type"]?.jsonPrimitive?.content ?: throw IllegalArgumentException("Missing action type"),
                parameters = actionObj["parameters"]?.jsonObject?.let { parseMetadata(it) } ?: emptyMap(),
                priority = actionObj["priority"]?.jsonPrimitive?.content ?: "MEDIUM"
            )
        }
    }

    private fun parseMetadata(metadata: JsonObject): Map<String, String> {
        return buildMap {
            metadata.entries.forEach { (key, value) ->
                when (value) {
                    is JsonPrimitive -> put(key, value.content)
                    is JsonObject, is JsonArray -> put(key, value.toString())
                }
            }
        }
    }
}
