package com.example.alertapp.android.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.alertapp.android.R
import com.example.alertapp.android.ui.viewmodels.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel = hiltViewModel()
) {
    var showAddAlertDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    IconButton(onClick = { /* TODO: Open settings */ }) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddAlertDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add Alert")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            AlertList(
                alerts = viewModel.alerts.collectAsState().value,
                onAlertClick = { /* TODO: Open alert details */ },
                onAlertToggle = { alert, enabled ->
                    viewModel.toggleAlert(alert, enabled)
                }
            )
        }

        if (showAddAlertDialog) {
            AddAlertDialog(
                onDismiss = { showAddAlertDialog = false },
                onConfirm = { alert ->
                    viewModel.addAlert(alert)
                    showAddAlertDialog = false
                }
            )
        }
    }
}
