package com.example.alertapp.services

import com.example.alertapp.nlp.AlertContext
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class OpenAIService : KoinComponent {
    private val httpClient: HttpClient by inject()
    private val apiKey: String by inject()

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
        You are an AI assistant specializing in creating intelligent alerts from natural language input.
        Your role is to understand user requests and convert them into structured alert configurations.
        
        Core Alert Structure:
        {
          "type": "string",           // Main alert category
          "name": "string",           // Brief, descriptive name
          "description": "string",    // Detailed explanation
          "trigger": {                // Trigger conditions
            "conditions": [           // List of conditions that trigger the alert
              {
                "type": "string",     // Type of condition (threshold, pattern, event, etc.)
                "operator": "string", // How to evaluate the condition
                "value": "string",    // Target value or pattern
                "metadata": {}        // Additional type-specific data
              }
            ],
            "logic": "string",       // How to combine conditions: AND, OR, CUSTOM
            "frequency": "string",   // How often to check: CONTINUOUS, SCHEDULED, EVENT_BASED
            "schedule": {            // Optional scheduling information
              "type": "string",     // RECURRING, ONE_TIME, ADAPTIVE
              "pattern": "string",  // Cron expression or natural language
              "timezone": "string" // User's timezone
            }
          },
          "sources": [              // Data sources to monitor
            {
              "type": "string",    // API, SENSOR, DATABASE, FILE, etc.
              "url": "string",     // Source URL or endpoint
              "parameters": {},    // Source-specific parameters
              "auth": {}          // Authentication details (if needed)
            }
          ],
          "actions": [            // What to do when triggered
            {
              "type": "string",  // NOTIFICATION, WEBHOOK, EMAIL, etc.
              "parameters": {},  // Action-specific parameters
              "priority": "string" // CRITICAL, HIGH, MEDIUM, LOW
            }
          ],
          "metadata": {}        // Additional alert properties
        }
    """.trimIndent()

    suspend fun analyzeContent(content: String): ContentAnalysis {
        val response = httpClient.post("https://api.openai.com/v1/chat/completions") {
            contentType(ContentType.Application.Json)
            header("Authorization", "Bearer $apiKey")
            setBody(buildJsonObject {
                put("model", "gpt-4-1106-preview")
                put("messages", buildJsonArray {
                    add(buildJsonObject {
                        put("role", "system")
                        put("content", systemPrompt)
                    })
                    add(buildJsonObject {
                        put("role", "user")
                        put("content", content)
                    })
                })
                put("temperature", 0.7)
                put("max_tokens", 1000)
            })
        }

        val responseBody = response.body<JsonObject>()
        val content = responseBody["choices"]?.jsonArray?.firstOrNull()
            ?.jsonObject?.get("message")?.jsonObject?.get("content")?.jsonPrimitive?.content
            ?: throw IllegalStateException("Invalid response from OpenAI")

        return parseContentAnalysis(content)
    }

    suspend fun parseAlertRequest(input: String, previousContext: AlertContext? = null): AlertRequestResponse {
        val response = httpClient.post("https://api.openai.com/v1/chat/completions") {
            contentType(ContentType.Application.Json)
            header("Authorization", "Bearer $apiKey")
            setBody(buildJsonObject {
                put("model", "gpt-4-1106-preview")
                put("messages", buildJsonArray {
                    add(buildJsonObject {
                        put("role", "system")
                        put("content", alertSystemPrompt)
                    })
                    previousContext?.let { context ->
                        add(buildJsonObject {
                            put("role", "system")
                            put("content", "Previous context: ${context.toJson()}")
                        })
                    }
                    add(buildJsonObject {
                        put("role", "user")
                        put("content", input)
                    })
                })
                put("temperature", 0.3)
                put("max_tokens", 2000)
            })
        }

        val responseBody = response.body<JsonObject>()
        val content = responseBody["choices"]?.jsonArray?.firstOrNull()
            ?.jsonObject?.get("message")?.jsonObject?.get("content")?.jsonPrimitive?.content
            ?: throw IllegalStateException("Invalid response from OpenAI")

        return parseAlertResponse(content, previousContext)
    }

    private fun parseContentAnalysis(content: String): ContentAnalysis {
        val lines = content.lines()
        return ContentAnalysis(
            relevance = lines.find { it.startsWith("- Relevance") }?.substringAfter(": ")?.toDoubleOrNull() ?: 0.0,
            sentiment = lines.find { it.startsWith("- Sentiment") }?.substringAfter(": ")?.trim() ?: "NEUTRAL",
            keyTopics = lines.find { it.startsWith("- Key topics") }?.substringAfter(": ")?.split(",")?.map { it.trim() } ?: emptyList(),
            entities = lines.find { it.startsWith("- Named entities") }?.substringAfter(": ")?.split(",")?.map { it.trim() } ?: emptyList(),
            suggestedAlerts = lines.find { it.startsWith("- Suggested alerts") }?.substringAfter(": ")?.split(",")?.map { it.trim() } ?: emptyList(),
            recommendedSources = lines.find { it.startsWith("- Recommended sources") }?.substringAfter(": ")?.split(",")?.map { it.trim() }?.toSet() ?: emptySet(),
            patterns = lines.find { it.startsWith("- Identified patterns") }?.substringAfter(": ")?.split(",")?.map { it.trim() } ?: emptyList()
        )
    }

    private fun parseAlertResponse(content: String, previousContext: AlertContext?): AlertRequestResponse {
        val jsonContent = if (content.contains("```json")) {
            content.substringAfter("```json").substringBefore("```").trim()
        } else {
            content.trim()
        }

        return try {
            val needsMoreInfo = content.contains("needs_more_info", ignoreCase = true) ||
                    content.contains("need more information", ignoreCase = true)
            
            val suggestedPrompts = content.lines()
                .filter { it.startsWith("-") && (it.contains("?") || it.contains("please provide")) }
                .map { it.trim().removePrefix("-").trim() }

            val compoundAlerts = content.lines()
                .filter { it.startsWith("{") && it.endsWith("}") }
                .drop(1) // Skip the main alert

            AlertRequestResponse(
                parameters = jsonContent,
                needsMoreInfo = needsMoreInfo,
                suggestedPrompts = suggestedPrompts,
                compoundAlerts = compoundAlerts,
                context = previousContext?.copy(lastInput = content) ?: AlertContext(lastInput = content)
            )
        } catch (e: Exception) {
            throw IllegalStateException("Failed to parse alert response: ${e.message}")
        }
    }
}
