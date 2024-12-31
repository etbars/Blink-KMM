package com.example.alertapp.android.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.alertapp.models.Alert
import com.example.alertapp.models.AlertType
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddAlertDialog(
    onDismiss: () -> Unit,
    onConfirm: (Alert) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(AlertType.WEATHER) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create New Alert") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth()
                )

                AlertTypeSelector(
                    selectedType = selectedType,
                    onTypeSelected = { selectedType = it }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val alert = Alert(
                        id = UUID.randomUUID().toString(),
                        name = name,
                        description = description,
                        type = selectedType,
                        enabled = true,
                        createdAt = System.currentTimeMillis(),
                        updatedAt = System.currentTimeMillis()
                    )
                    onConfirm(alert)
                },
                enabled = name.isNotBlank() && description.isNotBlank()
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun AlertTypeSelector(
    selectedType: AlertType,
    onTypeSelected: (AlertType) -> Unit
) {
    Column {
        Text(
            text = "Alert Type",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
        AlertType.values().forEach { type ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                RadioButton(
                    selected = type == selectedType,
                    onClick = { onTypeSelected(type) }
                )
                Text(
                    text = type.name.lowercase()
                        .replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }
}
