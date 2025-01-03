package com.example.alertapp.config

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class AppConfig(
    val apiKeys: Map<String, String> = emptyMap(),
    val baseUrls: Map<String, String> = emptyMap(),
    val defaultSettings: Map<String, String> = emptyMap(),
    val debug: Boolean = false
) {
    companion object {
        private val json = Json { 
            ignoreUnknownKeys = true 
            prettyPrint = true
        }

        fun fromJson(jsonString: String): AppConfig {
            return try {
                json.decodeFromString<AppConfig>(jsonString)
            } catch (e: Exception) {
                AppConfig() // Return default config if parsing fails
            }
        }

        fun toJson(config: AppConfig): String {
            return json.encodeToString(serializer(), config)
        }
    }

    fun getApiKey(service: String): String? = apiKeys[service]
    
    fun getBaseUrl(service: String): String? = baseUrls[service]
    
    fun getSetting(key: String): String? = defaultSettings[key]
    
    fun isDebugEnabled(): Boolean = debug
}
