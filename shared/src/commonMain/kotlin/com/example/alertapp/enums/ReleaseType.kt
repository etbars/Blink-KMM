package com.example.alertapp.enums

import kotlinx.serialization.Serializable

@Serializable
enum class ReleaseType {
    STABLE,
    BETA,
    ALPHA,
    RC,
    NIGHTLY,
    DEVELOPMENT
}
