package com.example.alertapp.services.openai

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

class OpenAIClient(
    private val config: OpenAIConfig,
    private val httpClient: HttpClient = HttpClient {
        install(ContentNegotiation) {
            json(Json {
                prettyPrint = true
                isLenient = true
                ignoreUnknownKeys = true
            })
        }
    }
) {
    private val baseUrl = "https://api.openai.com/v1"

    suspend fun chatCompletion(messages: List<ChatMessage>): ChatCompletionResponse {
        return httpClient.post("$baseUrl/chat/completions") {
            contentType(io.ktor.http.ContentType.Application.Json)
            header("Authorization", "Bearer ${config.apiKey}")
            setBody(ChatCompletionRequest(
                model = config.model,
                messages = messages,
                maxTokens = config.maxTokens,
                temperature = config.temperature,
                topP = config.topP,
                presencePenalty = config.presencePenalty,
                frequencyPenalty = config.frequencyPenalty
            ))
        }.body()
    }
}

@kotlinx.serialization.Serializable
data class ChatCompletionRequest(
    val model: String,
    val messages: List<ChatMessage>,
    val maxTokens: Int? = null,
    val temperature: Double? = null,
    val topP: Double? = null,
    val presencePenalty: Double? = null,
    val frequencyPenalty: Double? = null
)

@kotlinx.serialization.Serializable
data class ChatCompletionResponse(
    val id: String,
    val choices: List<ChatCompletionChoice>,
    val usage: TokenUsage? = null
)

@kotlinx.serialization.Serializable
data class ChatCompletionChoice(
    val index: Int,
    val message: ChatMessage,
    val finishReason: String? = null
)

@kotlinx.serialization.Serializable
data class TokenUsage(
    val promptTokens: Int,
    val completionTokens: Int,
    val totalTokens: Int
)
