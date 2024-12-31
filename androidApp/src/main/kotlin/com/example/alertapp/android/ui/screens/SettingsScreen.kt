package com.example.alertapp.android.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.alertapp.android.ui.viewmodels.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val settings by viewModel.settings.collectAsState()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Notifications Section
            SettingsSection(title = "Notifications") {
                SwitchPreference(
                    title = "Enable Notifications",
                    description = "Show notifications when alerts are triggered",
                    checked = settings.notificationsEnabled,
                    onCheckedChange = viewModel::setNotificationsEnabled
                )
                
                SwitchPreference(
                    title = "Vibrate",
                    description = "Vibrate when notifications are received",
                    checked = settings.vibrateEnabled,
                    onCheckedChange = viewModel::setVibrateEnabled
                )
            }

            // Background Updates Section
            SettingsSection(title = "Background Updates") {
                SwitchPreference(
                    title = "Background Sync",
                    description = "Keep alerts up to date in the background",
                    checked = settings.backgroundSyncEnabled,
                    onCheckedChange = viewModel::setBackgroundSyncEnabled
                )
                
                DropDownPreference(
                    title = "Sync Interval",
                    description = "How often to check for updates",
                    value = settings.syncInterval,
                    options = listOf("15 minutes", "30 minutes", "1 hour", "2 hours"),
                    onValueChange = viewModel::setSyncInterval,
                    enabled = settings.backgroundSyncEnabled
                )
            }

            // Backup Section
            SettingsSection(title = "Backup & Sync") {
                SwitchPreference(
                    title = "Auto Backup",
                    description = "Automatically backup alerts and settings",
                    checked = settings.autoBackupEnabled,
                    onCheckedChange = viewModel::setAutoBackupEnabled
                )

                ButtonPreference(
                    title = "Backup Now",
                    description = "Create a manual backup of your data",
                    buttonText = "Backup",
                    onClick = viewModel::createBackup
                )

                ButtonPreference(
                    title = "Restore Backup",
                    description = "Restore data from a previous backup",
                    buttonText = "Restore",
                    onClick = viewModel::restoreBackup
                )
            }

            // Data Usage Section
            SettingsSection(title = "Data Usage") {
                SwitchPreference(
                    title = "WiFi Only",
                    description = "Only sync when connected to WiFi",
                    checked = settings.wifiOnlyEnabled,
                    onCheckedChange = viewModel::setWifiOnlyEnabled
                )
            }
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(vertical = 8.dp)
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                content = content
            )
        }
    }
}

@Composable
private fun SwitchPreference(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.titleSmall)
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DropDownPreference(
    title: String,
    description: String,
    value: String,
    options: List<String>,
    onValueChange: (String) -> Unit,
    enabled: Boolean = true
) {
    var expanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.titleSmall)
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { if (enabled) expanded = it }
        ) {
            OutlinedTextField(
                value = value,
                onValueChange = {},
                readOnly = true,
                enabled = enabled,
                modifier = Modifier.menuAnchor(),
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) }
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option) },
                        onClick = {
                            onValueChange(option)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ButtonPreference(
    title: String,
    description: String,
    buttonText: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.titleSmall)
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Button(onClick = onClick) {
            Text(buttonText)
        }
    }
}
