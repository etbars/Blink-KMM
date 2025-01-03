package com.example.alertapp.network

import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
actual class PlatformNetworkClient @Inject constructor() : NetworkClient {
    companion object {
        const val TIMEOUT_MS = 30_000L // 30 seconds
    }

    override val client = HttpClient(OkHttp) {
        install(ContentNegotiation) {
            json(Json {
                prettyPrint = true
                isLenient = true
                ignoreUnknownKeys = true
            })
        }
        
        install(Logging) {
            logger = Logger.DEFAULT
            level = LogLevel.HEADERS
        }
        
        install(HttpTimeout) {
            requestTimeoutMillis = TIMEOUT_MS
            connectTimeoutMillis = TIMEOUT_MS
            socketTimeoutMillis = TIMEOUT_MS
        }
        
        engine {
            config {
                retryOnConnectionFailure(true)
                connectTimeout(TIMEOUT_MS, TimeUnit.MILLISECONDS)
                readTimeout(TIMEOUT_MS, TimeUnit.MILLISECONDS)
                writeTimeout(TIMEOUT_MS, TimeUnit.MILLISECONDS)
            }
        }
    }
}
