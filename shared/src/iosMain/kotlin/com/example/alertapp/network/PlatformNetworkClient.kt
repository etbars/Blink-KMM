package com.example.alertapp.network

import io.ktor.client.*
import io.ktor.client.engine.darwin.*

actual class PlatformNetworkClient : NetworkClient {
    actual override val client = HttpClient(Darwin) {
        configureClient()
        
        engine {
            configureRequest {
                setAllowsCellularAccess(true)
            }
        }
    }
}
