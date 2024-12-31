package com.example.alertapp.android

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.example.alertapp.android.deeplink.DeepLinkHandler
import com.example.alertapp.android.deeplink.DeepLinkResult
import com.example.alertapp.android.navigation.AlertNavigation
import com.example.alertapp.android.navigation.Screen
import com.example.alertapp.android.permissions.PermissionManager
import com.example.alertapp.android.services.AlertService
import com.example.alertapp.android.ui.AlertAppTheme
import com.example.alertapp.android.ui.screens.PermissionScreen
import com.example.alertapp.config.ConfigManager
import com.example.alertapp.services.AlertProcessor
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import android.widget.Toast
import java.util.UUID

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var configManager: ConfigManager
    @Inject lateinit var permissionManager: PermissionManager
    @Inject lateinit var deepLinkHandler: DeepLinkHandler
    @Inject lateinit var alertProcessor: AlertProcessor

    private var navController: NavHostController? = null

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.all { it.value }
        if (allGranted) {
            startAlertService()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            AlertAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    var hasPermissions by remember { mutableStateOf(permissionManager.hasAllPermissions()) }

                    LaunchedEffect(hasPermissions) {
                        if (hasPermissions) {
                            startAlertService()
                        }
                    }

                    if (hasPermissions) {
                        val navController = rememberNavController().also {
                            this.navController = it
                        }
                        AlertNavigation(navController = navController)
                    } else {
                        PermissionScreen(
                            permissionManager = permissionManager,
                            onRequestPermissions = {
                                requestPermissions()
                            }
                        )
                    }
                }
            }
        }

        // Handle intent
        handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        intent ?: return
        
        when (val result = deepLinkHandler.handleIntent(intent)) {
            is DeepLinkResult.ViewAlert -> {
                navController?.navigate(Screen.AlertDetail.createRoute(result.alertId))
            }
            is DeepLinkResult.ShareAlert -> {
                // Create a new alert from shared data
                val alert = Alert(
                    id = UUID.randomUUID().toString(),
                    name = result.name,
                    description = result.description,
                    type = result.type,
                    enabled = true,
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )
                // Save the alert
                lifecycleScope.launch {
                    alertProcessor.addAlert(alert)
                    // Navigate to the new alert
                    navController?.navigate(Screen.AlertDetail.createRoute(alert.id))
                }
            }
            is DeepLinkResult.Error -> {
                // Show error toast or snackbar
                Toast.makeText(this, result.message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun requestPermissions() {
        val permissions = permissionManager.getMissingPermissions()
        if (permissions.isNotEmpty()) {
            permissionLauncher.launch(permissions.toTypedArray())
        }
    }

    private fun startAlertService() {
        AlertService.startService(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        // Don't stop the service when the activity is destroyed
        // The service should continue running in the background
    }
}
