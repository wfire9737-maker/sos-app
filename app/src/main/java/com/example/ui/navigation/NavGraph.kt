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
import com.example.ui.screens.TrustedPlacesScreen
import com.example.ui.screens.AddEditTrustedPlaceScreen
import com.example.ui.screens.SecurityScreen
import com.example.ui.screens.AnalyticsScreen
import com.example.ui.screens.ReportsScreen
import com.example.ui.screens.SplashScreen
import com.example.ui.screens.OnboardingScreen
import com.example.ui.screens.TrustedPlacesScreen
import com.example.ui.screens.SosCountdownDialog
import com.example.ui.screens.FallCountdownDialog
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import com.example.ui.screens.SafeCheckInScreen
import com.example.ui.screens.PermissionsScreen
import com.example.ui.screens.AboutScreen
import com.example.ui.screens.DeveloperDashboardScreen
import com.example.ui.screens.AIScreen
import com.example.ui.screens.FallDetectionScreen
import com.example.ui.screens.VoiceSosScreen
import com.example.ui.screens.SafetyTimerScreen

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
    val startDestination = Screen.Splash.route

    val fallState by viewModel.fallState.collectAsState()
    val fallCountdown by viewModel.fallCountdown.collectAsState()
    val sosCountdown by viewModel.countdown.collectAsState()
    val activeEmergency by viewModel.activeEmergency.collectAsState()
    LaunchedEffect(activeEmergency) {
        if (activeEmergency != null) {
            val currentRoute = navController.currentBackStackEntry?.destination?.route
            if (currentRoute != Screen.Emergency.route) {
                navController.navigate(Screen.Emergency.route)
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.Splash.route) {
            SplashScreen(
                viewModel = viewModel,
                onNavigateToNext = {
                    val currentAuth = viewModel.authState.value
                    if (currentAuth is AuthState.Success) {
                        navController.navigate(Screen.Home.route) { popUpTo(0) { inclusive = true } }
                    } else {
                        navController.navigate(Screen.Onboarding.route) { popUpTo(0) { inclusive = true } }
                    }
                }
            )
        }
        composable(Screen.Onboarding.route) {
            OnboardingScreen(
                viewModel = viewModel,
                onFinishOnboarding = {
                    navController.navigate(Screen.Login.route) { popUpTo(Screen.Onboarding.route) { inclusive = true } }
                }
            )
        }
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
                onNavigateToLogin = { navController.popBackStack() }
            )
        }
        composable(Screen.ForgotPassword.route) {
            ForgotPasswordScreen(
                viewModel = viewModel,
                onNavigateToLogin = { navController.popBackStack() }
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
                },
                onNavigateToSafeCheckIn = {
                    navController.navigate(Screen.SafeCheckIn.route)
                },
                onNavigateToMap = {
                    navController.navigate(Screen.Map.route)
                },
                onNavigateToBleTest = {
                    navController.navigate(Screen.BleTest.route)
                }
            )
        }
        composable(Screen.Profile.route) {
            ProfileScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.navigateUp() }
            )
        }
        composable(Screen.Contacts.route) {
            ContactsScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.navigateUp() }
            )
        }
        composable(Screen.DevicePairing.route) {
            DevicePairingScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.navigateUp() }
            )
        }
        composable(Screen.Map.route) {
            MapScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.navigateUp() }
            )
        }
        composable(Screen.Emergency.route) {
            EmergencyScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.navigateUp() },
                )
        }
        composable(Screen.Notifications.route) {
            NotificationScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.navigateUp() }
            )
        }
        composable(Screen.History.route) {
            EmergencyHistoryScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.navigateUp() }
            )
        }
        composable(Screen.AiDashboard.route) {
            AiDashboardScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.navigateUp() }
            )
        }
        composable(Screen.DeviceMonitoring.route) {
            DeviceMonitoringScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.navigateUp() }
            )
        }
        composable(Screen.TrustedPlaces.route) {
            TrustedPlacesScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.navigateUp() },
                onNavigateToAddPlace = { navController.navigate(Screen.AddEditTrustedPlace.createRoute(null)) },
                onNavigateToEditPlace = { placeId -> navController.navigate(Screen.AddEditTrustedPlace.createRoute(placeId)) }
            )
        }
        composable(
            route = Screen.AddEditTrustedPlace.route,
            arguments = listOf(androidx.navigation.navArgument("placeId") { nullable = true; type = androidx.navigation.NavType.StringType })
        ) { backStackEntry ->
            val placeId = backStackEntry.arguments?.getString("placeId")?.takeIf { it.isNotBlank() }
            AddEditTrustedPlaceScreen(
                viewModel = viewModel,
                placeId = placeId,
                onNavigateBack = { navController.navigateUp() }
            )
        }
        composable(Screen.Settings.route) {
            SettingsScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.navigateUp() },
                onNavigateToNearbyDiscovery = { navController.navigate(Screen.NearbyDiscovery.route) },
                onNavigateToSecurity = { navController.navigate(Screen.Security.route) },
                onNavigateToFallDetection = { navController.navigate(Screen.FallDetection.route) },
                onNavigateToAnalytics = { navController.navigate(Screen.Analytics.route) },
                onNavigateToVoiceSos = { navController.navigate(Screen.VoiceSos.route) },
                onNavigateToSafetyTimer = { navController.navigate(Screen.SafetyTimer.route) },
                onNavigateToMap = { navController.navigate(Screen.Map.route) },
                onNavigateToQRCode = { navController.navigate(Screen.QRCode.route) },
                onNavigateToHelpFaq = { navController.navigate(Screen.HelpFaq.route) },
                onNavigateToAiScreen = { navController.navigate(Screen.AiScreen.route) },
                onNavigateToTrustedPlaces = { navController.navigate(Screen.TrustedPlaces.route) },
                onNavigateToPermissions = { navController.navigate(Screen.Permissions.route) },
                onNavigateToAbout = { navController.navigate(Screen.About.route) },
                onNavigateToDeveloperDashboard = { navController.navigate(Screen.DeveloperDashboard.route) }
            )
        }
        composable(Screen.Security.route) {
            SecurityScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.navigateUp() }
            )
        }
        composable(Screen.Analytics.route) {
            AnalyticsScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.navigateUp() }
            )
        }
        composable(Screen.Reports.route) {
            ReportsScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.navigateUp() }
            )
        }
        composable(Screen.AiScreen.route) {
            AIScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.navigateUp() }
            )
        }
        composable(Screen.FallDetection.route) {
            FallDetectionScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.navigateUp() }
            )
        }
        composable(Screen.VoiceSos.route) {
            VoiceSosScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.navigateUp() }
            )
        }
        composable(Screen.SafetyTimer.route) {
            SafetyTimerScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.navigateUp() }
            )
        }
        composable(Screen.QRCode.route) {
            com.example.ui.screens.QRCodeScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.navigateUp() }
            )
        }
        composable(Screen.HelpFaq.route) {
            com.example.ui.screens.HelpFaqScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.navigateUp() }
            )
        }
        composable(Screen.SafeCheckIn.route) {
            SafeCheckInScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.navigateUp() }
            )
        }
        composable(Screen.Permissions.route) {
            PermissionsScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.navigateUp() }
            )
        }
        composable(Screen.About.route) {
            AboutScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.navigateUp() }
            )

        }
        composable(Screen.DeveloperDashboard.route) {
            DeveloperDashboardScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.navigateUp() }
            )
        }
        composable(Screen.BleTest.route) {
            com.example.ui.screens.BleTestScreen(
                bleManager = viewModel.deviceService.bleManager,
                onNavigateBack = { navController.navigateUp() }
            )
        }
        composable(Screen.NearbyDiscovery.route) {
            com.example.ui.screens.NearbyDiscoveryScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.navigateUp() }
            )
        }
    }

    if (fallState == "FALL_COUNTDOWN") {
        FallCountdownDialog(
            secondsLeft = fallCountdown,
            onCancel = { viewModel.fallDetectionService.cancelFallCountdown() }
        )
    }

    if (sosCountdown != null) {
        SosCountdownDialog(
            secondsLeft = sosCountdown!!,
            onCancel = { viewModel.cancelEmergencyWithPin("") {} }
        )
    }
}

