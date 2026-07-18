package com.example.ui.navigation

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Register : Screen("register")
    object ForgotPassword : Screen("forgot_password")
    object Home : Screen("home")
    object Profile : Screen("profile")
    object Contacts : Screen("contacts")
    object DevicePairing : Screen("device_pairing")
    object Map : Screen("map")
    object Emergency : Screen("emergency")
    object Notifications : Screen("notifications")
    object History : Screen("history")
    object AiDashboard : Screen("ai_dashboard")
    object DeviceMonitoring : Screen("device_monitoring")
    object Settings : Screen("settings")
    object Security : Screen("security")
    object Analytics : Screen("analytics")
    object Reports : Screen("reports")
    object AiScreen : Screen("ai_screen")
    object FallDetection : Screen("fall_detection")
    object VoiceSos : Screen("voice_sos")
}
