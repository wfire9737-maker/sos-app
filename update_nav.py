import os

filepath = "app/src/main/java/com/example/ui/navigation/NavGraph.kt"
with open(filepath, "r") as f:
    content = f.read()

old_settings_nav = """        composable(Screen.Settings.route) {
            SettingsScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToSecurity = { navController.navigate(Screen.Security.route) },
                onNavigateToFallDetection = { navController.navigate(Screen.FallDetection.route) },
                onNavigateToAnalytics = { navController.navigate(Screen.Analytics.route) },
                )
        }"""
new_settings_nav = """        composable(Screen.Settings.route) {
            SettingsScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToSecurity = { navController.navigate(Screen.Security.route) },
                onNavigateToFallDetection = { navController.navigate(Screen.FallDetection.route) },
                onNavigateToAnalytics = { navController.navigate(Screen.Analytics.route) },
                onNavigateToVoiceSos = { navController.navigate(Screen.VoiceSos.route) },
                onNavigateToSafetyTimer = { navController.navigate(Screen.SafetyTimer.route) },
                onNavigateToMap = { navController.navigate(Screen.Map.route) },
                onNavigateToQRCode = { navController.navigate(Screen.QRCode.route) },
                onNavigateToHelpFaq = { navController.navigate(Screen.HelpFaq.route) },
                onNavigateToAiScreen = { navController.navigate(Screen.AiScreen.route) }
            )
        }"""
content = content.replace(old_settings_nav, new_settings_nav)

with open(filepath, "w") as f:
    f.write(content)

