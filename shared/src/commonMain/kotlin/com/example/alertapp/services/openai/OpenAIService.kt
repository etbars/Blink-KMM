package com.example.alertapp.services.openai

import com.example.alertapp.models.AlertContext
import kotlinx.serialization.json.*
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import co.touchlab.kermit.Logger

class OpenAIService(
    private val config: OpenAIConfig,
    private val client: OpenAIClient = OpenAIClient(config)
) {
    private val logger = Logger.withTag("OpenAIService")

    data class ContentAnalysis(
        val relevance: Double,
        val sentiment: String,
        val keyTopics: List<String>,
        val entities: List<String>,
        val suggestedAlerts: List<String>,
        val recommendedSources: Set<String>,
        val patterns: List<String>
    )

    data class AlertRequestResponse(
        val parameters: String,
        val needsMoreInfo: Boolean = false,
        val suggestedPrompts: List<String> = emptyList(),
        val compoundAlerts: List<String> = emptyList(),
        val context: AlertContext
    )

    private val systemPrompt = """
        You are an AI assistant helping to analyze content and generate alerts.
        Provide analysis in a structured format with the following:
        - Relevance score (0.0 to 1.0)
        - Sentiment (POSITIVE, NEGATIVE, NEUTRAL)
        - Key topics (comma-separated list)
        - Named entities (comma-separated list)
        - Suggested alerts (comma-separated list)
        - Recommended sources (comma-separated list)
        - Identified patterns (comma-separated list)
    """.trimIndent()

    private val alertSystemPrompt = """
        You are an AI assistant helping to create alerts from natural language input.
        Convert user requests into structured alert configurations with the following types:
        - PRICE: For price-based alerts (stocks, crypto, etc.)
        - CONTENT: For content-based alerts (news, social media, etc.)
        - WEATHER: For weather condition alerts
        - RELEASE: For new release alerts (music, movies, software)
        - EVENT: For event-based alerts
        - CUSTOM: For custom alert types
        
        For PRICE alerts, use this format:
        {
          "type": "PRICE",
          "name": "Brief alert name",
          "description": "Detailed description",
          "trigger": {
            "asset": "asset name or symbol",
            "condition": "ABOVE|BELOW|INCREASES_BY|DECREASES_BY",
            "threshold": numeric_value_without_currency_symbol,
            "timeframe": "timeframe in format: 5m, 15m, 1h, 4h, 1d, 1w"
          }
        }

        For WEATHER alerts, use this format:
        {
          "type": "WEATHER",
          "name": "Brief alert name",
          "description": "Detailed description",
          "trigger": {
            "location": "city or location name",
            "conditions": [
              {
                "metric": "TEMPERATURE|HUMIDITY|PRESSURE|WIND_SPEED|PRECIPITATION|UV_INDEX",
                "operator": "ABOVE|BELOW|EQUALS|NOT_EQUALS",
                "value": numeric_value,
                "duration": hours_as_integer_optional
              }
            ],
            "logicOperator": "AND|OR" (optional, defaults to OR)
          }
        }

        For CONTENT alerts, use this format:
        {
          "type": "CONTENT",
          "name": "Brief alert name",
          "description": "Detailed description",
          "trigger": {
            "sources": ["source1", "source2"],
            "keywords": ["keyword1", "keyword2"],
            "categories": ["category1", "category2"],
            "sentiment": "POSITIVE|NEGATIVE|NEUTRAL" (optional),
            "frequency": "IMMEDIATELY|HOURLY|DAILY|WEEKLY" (optional)
          }
        }
        
        If more information is needed, set needsMoreInfo to true and provide suggestedPrompts.
    """.trimIndent()

    suspend fun parseAlertRequest(
        input: String,
        previousContext: AlertContext? = null
    ): AlertRequestResponse = withContext(Dispatchers.Default) {
        try {
            val contextInfo = previousContext?.let {
                "\nPrevious context:\n" +
                "- Patterns: ${it.patterns.joinToString(", ") { pattern -> pattern.value }}\n" +
                "- Metadata: ${Json.encodeToString(JsonObject.serializer(), JsonObject(it.metadata.mapValues { JsonPrimitive(it.value.toString()) }))}"
            } ?: ""

            logger.d { "Sending request to OpenAI with input: $input" }
            
            val completion = client.chatCompletion(listOf(
                ChatMessage(
                    role = ChatRole.SYSTEM,
                    content = alertSystemPrompt
                ),
                ChatMessage(
                    role = ChatRole.USER,
                    content = "$input$contextInfo"
                )
            ))

            val response = completion.choices.firstOrNull()?.message?.content 
                ?: throw IllegalStateException("Empty response from OpenAI")
            
            logger.d { "Received response from OpenAI: $response" }
            
            val responseJson = Json.parseToJsonElement(response).jsonObject

            AlertRequestResponse(
                parameters = response,
                needsMoreInfo = responseJson["needsMoreInfo"]?.jsonPrimitive?.booleanOrNull ?: false,
                suggestedPrompts = responseJson["suggestedPrompts"]?.jsonArray?.map { it.jsonPrimitive.content } ?: emptyList(),
                compoundAlerts = responseJson["compoundAlerts"]?.jsonArray?.map { it.toString() } ?: emptyList(),
                context = previousContext ?: AlertContext.create("")
            )
        } catch (e: Exception) {
            logger.e(e) { "Error parsing alert request: ${e.message}" }
            AlertRequestResponse(
                parameters = "{}",
                needsMoreInfo = true,
                suggestedPrompts = listOf(
                    "Could you please provide more details about the alert you want to create?",
                    "I encountered an error: ${e.message}. Please try again or rephrase your request."
                ),
                context = previousContext ?: AlertContext.create("")
            )
        }
    }

    suspend fun analyzeContent(content: String): ContentAnalysis = withContext(Dispatchers.Default) {
        try {
            logger.d { "Analyzing content with OpenAI" }
            
            val completion = client.chatCompletion(listOf(
                ChatMessage(
                    role = ChatRole.SYSTEM,
                    content = systemPrompt
                ),
                ChatMessage(
                    role = ChatRole.USER,
                    content = content
                )
            ))

            val response = completion.choices.firstOrNull()?.message?.content 
                ?: throw IllegalStateException("Empty response from OpenAI")
            
            logger.d { "Received analysis from OpenAI: $response" }
            
            parseAnalysis(response)
        } catch (e: Exception) {
            logger.e(e) { "Error analyzing content: ${e.message}" }
            ContentAnalysis(
                relevance = 0.0,
                sentiment = "NEUTRAL",
                keyTopics = emptyList(),
                entities = emptyList(),
                suggestedAlerts = emptyList(),
                recommendedSources = emptySet(),
                patterns = emptyList()
            )
        }
    }

    private fun parseAnalysis(response: String): ContentAnalysis {
        val lines = response.lines()
        var relevance = 0.0
        var sentiment = "NEUTRAL"
        var keyTopics = emptyList<String>()
        var entities = emptyList<String>()
        var suggestedAlerts = emptyList<String>()
        var recommendedSources = emptySet<String>()
        var patterns = emptyList<String>()

        lines.forEach { line ->
            when {
                line.startsWith("Relevance score:") -> {
                    relevance = line.substringAfter(":").trim().toDoubleOrNull() ?: 0.0
                }
                line.startsWith("Sentiment:") -> {
                    sentiment = line.substringAfter(":").trim()
                }
                line.startsWith("Key topics:") -> {
                    keyTopics = line.substringAfter(":").split(",").map { it.trim() }
                }
                line.startsWith("Named entities:") -> {
                    entities = line.substringAfter(":").split(",").map { it.trim() }
                }
                line.startsWith("Suggested alerts:") -> {
                    suggestedAlerts = line.substringAfter(":").split(",").map { it.trim() }
                }
                line.startsWith("Recommended sources:") -> {
                    recommendedSources = line.substringAfter(":").split(",").map { it.trim() }.toSet()
                }
                line.startsWith("Identified patterns:") -> {
                    patterns = line.substringAfter(":").split(",").map { it.trim() }
                }
            }
        }

        return ContentAnalysis(
            relevance = relevance,
            sentiment = sentiment,
            keyTopics = keyTopics,
            entities = entities,
            suggestedAlerts = suggestedAlerts,
            recommendedSources = recommendedSources,
            patterns = patterns
        )
    }
}
