package com.example.alertapp.android.permissions

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PermissionManager @Inject constructor(
    private val context: Context
) {
    fun requiredPermissions(): List<String> {
        return buildList {
            // Location permissions
            add(Manifest.permission.ACCESS_FINE_LOCATION)
            add(Manifest.permission.ACCESS_COARSE_LOCATION)
            
            // Notification permission (required for Android 13+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    fun getMissingPermissions(): List<String> {
        return requiredPermissions().filter { permission ->
            ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED
        }
    }

    fun hasAllPermissions(): Boolean {
        return getMissingPermissions().isEmpty()
    }

    fun shouldShowRationale(permission: String): Boolean {
        return when (permission) {
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION -> {
                "Location permission is needed to monitor location-based alerts and provide accurate weather information."
            }
            Manifest.permission.POST_NOTIFICATIONS -> {
                "Notification permission is needed to alert you when your configured conditions are met."
            }
            else -> null
        } != null
    }

    fun getPermissionRationale(permission: String): String {
        return when (permission) {
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION -> {
                "Location permission is needed to monitor location-based alerts and provide accurate weather information."
            }
            Manifest.permission.POST_NOTIFICATIONS -> {
                "Notification permission is needed to alert you when your configured conditions are met."
            }
            else -> "This permission is required for the app to function properly."
        }
    }
}
