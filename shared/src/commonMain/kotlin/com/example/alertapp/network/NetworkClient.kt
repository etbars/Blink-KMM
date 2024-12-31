package com.example.alertapp.network

import io.ktor.client.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.defaultrequest.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

interface NetworkClient {
    val client: HttpClient
}

expect class PlatformNetworkClient : NetworkClient

fun HttpClientConfig<*>.configureClient() {
    install(ContentNegotiation) {
        json(Json {
            prettyPrint = true
            isLenient = true
            ignoreUnknownKeys = true
        })
    }
    
    install(DefaultRequest) {
        header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
    }
}
