package com.example.alertapp.android.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.alertapp.android.permissions.PermissionManager

@Composable
fun PermissionScreen(
    permissionManager: PermissionManager,
    onRequestPermissions: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Required Permissions",
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "This app needs the following permissions to function properly:",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        permissionManager.getMissingPermissions().forEach { permission ->
            PermissionItem(
                title = getPermissionTitle(permission),
                description = permissionManager.getPermissionRationale(permission)
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Button(
            onClick = onRequestPermissions,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Grant Permissions")
        }
    }
}

@Composable
private fun PermissionItem(
    title: String,
    description: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

private fun getPermissionTitle(permission: String): String {
    return when {
        permission.contains("LOCATION") -> "Location"
        permission.contains("NOTIFICATIONS") -> "Notifications"
        else -> permission.split(".").last().split("_").joinToString(" ").lowercase()
            .replaceFirstChar { it.uppercase() }
    }
}
