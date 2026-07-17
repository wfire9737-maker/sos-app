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
}
