package com.example.alertapp.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class AlertType {
    @SerialName("price")
    PRICE,
    
    @SerialName("content")
    CONTENT,
    
    @SerialName("weather")
    WEATHER,
    
    @SerialName("release")
    RELEASE,
    
    @SerialName("event")
    EVENT,
    
    @SerialName("custom")
    CUSTOM
}
