package com.example.alertapp.android.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.alertapp.android.R
import com.example.alertapp.android.ui.viewmodels.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToSettings: () -> Unit,
    onNavigateToAddAlert: () -> Unit,
    onNavigateToAlertDetail: (String) -> Unit,
    viewModel: MainViewModel = hiltViewModel()
) {
    val alerts by viewModel.alerts.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    IconButton(onClick = {
                        onNavigateToSettings()
                    }) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                    IconButton(onClick = {
                        /* Navigate to history */
                    }) {
                        Icon(Icons.Default.History, contentDescription = "History")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onNavigateToAddAlert) {
                Icon(Icons.Default.Add, contentDescription = "Add Alert")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            AlertList(
                alerts = alerts,
                onAlertClick = onNavigateToAlertDetail,
                onAlertToggle = { alert, enabled ->
                    viewModel.toggleAlert(alert, enabled)
                }
            )
        }
    }
}
