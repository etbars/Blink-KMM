package com.example.alertapp.services.openai

import kotlinx.serialization.Serializable

@Serializable
data class OpenAIConfig(
    val apiKey: String,
    val model: String = "gpt-3.5-turbo",
    val maxTokens: Int = 1000,
    val temperature: Double = 0.7,
    val topP: Double = 1.0,
    val presencePenalty: Double = 0.0,
    val frequencyPenalty: Double = 0.0,
)

@Serializable
data class ChatMessage(
    val role: ChatRole,
    val content: String
)

@Serializable
enum class ChatRole {
    SYSTEM, USER, ASSISTANT;

    override fun toString(): String = name.lowercase()
}
