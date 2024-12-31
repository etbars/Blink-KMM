package com.example.alertapp.android.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.alertapp.android.ui.viewmodels.AlertDetailViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlertDetailScreen(
    alertId: String,
    onNavigateBack: () -> Unit,
    onNavigateToEdit: () -> Unit,
    viewModel: AlertDetailViewModel = hiltViewModel()
) {
    val alert by viewModel.alert.collectAsState()
    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(alertId) {
        viewModel.loadAlert(alertId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(alert?.name ?: "") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToEdit) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit")
                    }
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete")
                    }
                    IconButton(onClick = { 
                        alert?.let { currentAlert ->
                            val shareIntent = Intent.createChooser(Intent().apply {
                                action = Intent.ACTION_SEND
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, viewModel.getShareText(currentAlert))
                            }, "Share Alert")
                            context.startActivity(shareIntent)
                        }
                    }) {
                        Icon(Icons.Default.Share, contentDescription = "Share")
                    }
                }
            )
        }
    ) { padding ->
        alert?.let { currentAlert ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Alert Type
                DetailItem(
                    icon = Icons.Default.Category,
                    label = "Type",
                    value = currentAlert.type.name
                )

                // Description
                DetailItem(
                    icon = Icons.Default.Description,
                    label = "Description",
                    value = currentAlert.description
                )

                // Status
                DetailItem(
                    icon = if (currentAlert.enabled) Icons.Default.CheckCircle 
                           else Icons.Default.Cancel,
                    label = "Status",
                    value = if (currentAlert.enabled) "Active" else "Inactive"
                )

                // Created Date
                DetailItem(
                    icon = Icons.Default.DateRange,
                    label = "Created",
                    value = formatDate(currentAlert.createdAt)
                )

                // Last Updated
                DetailItem(
                    icon = Icons.Default.Update,
                    label = "Last Updated",
                    value = formatDate(currentAlert.updatedAt)
                )

                Spacer(modifier = Modifier.weight(1f))

                // Enable/Disable Switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (currentAlert.enabled) "Disable Alert" else "Enable Alert",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Switch(
                        checked = currentAlert.enabled,
                        onCheckedChange = { enabled ->
                            viewModel.toggleAlert(enabled)
                        }
                    )
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Alert") },
            text = { Text("Are you sure you want to delete this alert?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteAlert()
                        showDeleteDialog = false
                        onNavigateBack()
                    }
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun DetailItem(
    icon: ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary
        )
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

private fun formatDate(timestamp: Long): String {
    val date = Date(timestamp)
    val format = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
    return format.format(date)
}
