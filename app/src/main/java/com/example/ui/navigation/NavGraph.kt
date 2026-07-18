package com.example.ui.navigation

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.service.AuthState
import com.example.ui.GuardianViewModel
import com.example.ui.screens.ForgotPasswordScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LoginScreen
import com.example.ui.screens.RegisterScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.ContactsScreen
import com.example.ui.screens.DevicePairingScreen
import com.example.ui.screens.MapScreen
import com.example.ui.screens.EmergencyScreen
import com.example.ui.screens.NotificationScreen
import com.example.ui.screens.EmergencyHistoryScreen
import com.example.ui.screens.AiDashboardScreen
import com.example.ui.screens.DeviceMonitoringScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SecurityScreen
import com.example.ui.screens.AnalyticsScreen
import com.example.ui.screens.ReportsScreen
import com.example.ui.screens.AIScreen
import com.example.ui.screens.FallDetectionScreen
import com.example.ui.screens.VoiceSosScreen

@Composable
fun NavGraph(
    navController: NavHostController = rememberNavController(),
    viewModel: GuardianViewModel = viewModel()
) {
    val context = LocalContext.current

    // Listen to ViewModel uiEvents for notifications & deep navigation redirects
    LaunchedEffect(key1 = Unit) {
        viewModel.uiEvents.collect { event ->
            when (event) {
                is GuardianViewModel.UiEvent.ShowToast -> {
                    Toast.makeText(context, event.message, Toast.LENGTH_LONG).show()
                }
                is GuardianViewModel.UiEvent.NavigateToHome -> {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
                is GuardianViewModel.UiEvent.NavigateToLogin -> {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
                is GuardianViewModel.UiEvent.NavigateToEmergency -> {
                    navController.navigate(Screen.Emergency.route)
                }
            }
        }
    }

    // Determine starting route depending on session availability
    val authState = viewModel.authState.value
    val startDestination = if (authState is AuthState.Success) Screen.Home.route else Screen.Login.route

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.Login.route) {
            LoginScreen(
                viewModel = viewModel,
                onNavigateToRegister = { navController.navigate(Screen.Register.route) },
                onNavigateToForgotPassword = { navController.navigate(Screen.ForgotPassword.route) }
            )
        }
        composable(Screen.Register.route) {
            RegisterScreen(
                viewModel = viewModel,
                onNavigateToLogin = { navController.navigate(Screen.Login.route) }
            )
        }
        composable(Screen.ForgotPassword.route) {
            ForgotPasswordScreen(
                viewModel = viewModel,
                onNavigateToLogin = { navController.navigate(Screen.Login.route) }
            )
        }
        composable(Screen.Home.route) {
            HomeScreen(
                viewModel = viewModel,
                onNavigateToLogin = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onNavigateToProfile = {
                    navController.navigate(Screen.Profile.route)
                },
                onNavigateToContacts = {
                    navController.navigate(Screen.Contacts.route)
                },
                onNavigateToDevicePairing = {
                    navController.navigate(Screen.DevicePairing.route)
                },
                onNavigateToMap = {
                    navController.navigate(Screen.Map.route)
                },
                onNavigateToEmergency = {
                    navController.navigate(Screen.Emergency.route)
                },
                onNavigateToNotifications = {
                    navController.navigate(Screen.Notifications.route)
                },
                onNavigateToHistory = {
                    navController.navigate(Screen.History.route)
                },
                onNavigateToAiDashboard = {
                    navController.navigate(Screen.AiDashboard.route)
                },
                onNavigateToDeviceMonitoring = {
                    navController.navigate(Screen.DeviceMonitoring.route)
                },
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
                },
                onNavigateToAnalytics = {
                    navController.navigate(Screen.Analytics.route)
                },
                onNavigateToReports = {
                    navController.navigate(Screen.Reports.route)
                }
            )
        }
        composable(Screen.Profile.route) {
            ProfileScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(Screen.Contacts.route) {
            ContactsScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(Screen.DevicePairing.route) {
            DevicePairingScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(Screen.Map.route) {
            MapScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(Screen.Emergency.route) {
            EmergencyScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToMap = {
                    navController.navigate(Screen.Map.route)
                }
            )
        }
        composable(Screen.Notifications.route) {
            NotificationScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(Screen.History.route) {
            EmergencyHistoryScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(Screen.AiDashboard.route) {
            AIScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(Screen.DeviceMonitoring.route) {
            DeviceMonitoringScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(Screen.Settings.route) {
            SettingsScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToSecurity = { navController.navigate(Screen.Security.route) },
                onNavigateToFallDetection = { navController.navigate(Screen.FallDetection.route) },
                onNavigateToVoiceSos = { navController.navigate(Screen.VoiceSos.route) },
                onNavigateToAiScreen = { navController.navigate(Screen.AiScreen.route) }
            )
        }
        composable(Screen.Security.route) {
            SecurityScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(Screen.Analytics.route) {
            AnalyticsScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(Screen.Reports.route) {
            ReportsScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(Screen.AiScreen.route) {
            AIScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(Screen.FallDetection.route) {
            FallDetectionScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(Screen.VoiceSos.route) {
            VoiceSosScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
