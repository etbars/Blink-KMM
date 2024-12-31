package com.example.alertapp.android.navigation

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Settings : Screen("settings")
    object AlertDetail : Screen("alert/{alertId}") {
        fun createRoute(alertId: String) = "alert/$alertId"
    }
    object AddAlert : Screen("add_alert")
    object EditAlert : Screen("edit_alert/{alertId}") {
        fun createRoute(alertId: String) = "edit_alert/$alertId"
    }
    object AlertHistory : Screen("alert_history")
}

sealed class AlertDetailArgs(val key: String) {
    object AlertId : AlertDetailArgs("alertId")
}
