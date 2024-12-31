package com.example.alertapp.network

import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
actual class PlatformNetworkClient @Inject constructor() : NetworkClient {
    actual override val client = HttpClient(OkHttp) {
        configureClient()
        
        engine {
            config {
                retryOnConnectionFailure(true)
                connectTimeout(NetworkClient.TIMEOUT_MS, TimeUnit.MILLISECONDS)
                readTimeout(NetworkClient.TIMEOUT_MS, TimeUnit.MILLISECONDS)
                writeTimeout(NetworkClient.TIMEOUT_MS, TimeUnit.MILLISECONDS)
            }
        }
    }
}
