package com.example.alertapp.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class ReleaseType {
    @SerialName("software")
    SOFTWARE,
    
    @SerialName("music")
    MUSIC,
    
    @SerialName("movie")
    MOVIE,
    
    @SerialName("book")
    BOOK,
    
    @SerialName("game")
    GAME,
    
    @SerialName("other")
    OTHER
}
