package com.example.alertapp.services

import com.example.alertapp.models.Release
import com.example.alertapp.models.ReleaseCondition
import com.example.alertapp.models.ReleaseMediaType
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReleaseService @Inject constructor() {
    suspend fun checkNewReleases(
        creator: String,
        mediaType: ReleaseMediaType,
        conditions: List<ReleaseCondition>
    ): List<Release> {
        // TODO: Implement actual release checking logic
        return emptyList()
    }
}
