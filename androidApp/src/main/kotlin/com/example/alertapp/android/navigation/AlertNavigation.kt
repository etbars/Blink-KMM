package com.example.alertapp.android.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.alertapp.android.ui.screens.*

@Composable
fun AlertNavigation(
    navController: NavHostController,
    startDestination: String = Screen.Home.route
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.Home.route) {
            HomeScreen(
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
                },
                onNavigateToAddAlert = {
                    navController.navigate(Screen.AddAlert.route)
                },
                onNavigateToAlertDetail = { alertId ->
                    navController.navigate(Screen.AlertDetail.createRoute(alertId))
                }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(
            route = Screen.AlertDetail.route,
            arguments = listOf(
                navArgument(AlertDetailArgs.AlertId.key) {
                    type = NavType.StringType
                }
            )
        ) { backStackEntry ->
            val alertId = backStackEntry.arguments?.getString(AlertDetailArgs.AlertId.key)
            requireNotNull(alertId) { "Alert ID is required" }
            
            AlertDetailScreen(
                alertId = alertId,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToEdit = {
                    navController.navigate(Screen.EditAlert.createRoute(alertId))
                }
            )
        }

        composable(Screen.AddAlert.route) {
            AddEditAlertScreen(
                alertId = null,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onSaved = {
                    navController.popBackStack()
                }
            )
        }

        composable(
            route = Screen.EditAlert.route,
            arguments = listOf(
                navArgument(AlertDetailArgs.AlertId.key) {
                    type = NavType.StringType
                }
            )
        ) { backStackEntry ->
            val alertId = backStackEntry.arguments?.getString(AlertDetailArgs.AlertId.key)
            requireNotNull(alertId) { "Alert ID is required" }
            
            AddEditAlertScreen(
                alertId = alertId,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onSaved = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.AlertHistory.route) {
            AlertHistoryScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}
