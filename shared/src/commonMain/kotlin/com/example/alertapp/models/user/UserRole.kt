package com.example.alertapp.models.user

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class UserRole {
    @SerialName("admin")
    ADMIN,
    
    @SerialName("moderator")
    MODERATOR,
    
    @SerialName("user")
    USER,
    
    @SerialName("guest")
    GUEST
}
