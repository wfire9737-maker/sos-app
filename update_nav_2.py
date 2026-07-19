import os

filepath = "app/src/main/java/com/example/ui/navigation/NavGraph.kt"
with open(filepath, "r") as f:
    content = f.read()

# Add imports for new screens
import_insert_pos = content.find("import com.example.ui.screens.AIScreen")
if import_insert_pos != -1:
    new_imports = """import com.example.ui.screens.SplashScreen
import com.example.ui.screens.OnboardingScreen
import com.example.ui.screens.TrustedPlacesScreen
import com.example.ui.screens.SafeCheckInScreen
import com.example.ui.screens.PermissionsScreen
import com.example.ui.screens.AboutScreen
"""
    content = content[:import_insert_pos] + new_imports + content[import_insert_pos:]

# Update startDestination
old_start = """    val startDestination = if (authState is AuthState.Success) Screen.Home.route else Screen.Login.route"""
new_start = """    val startDestination = Screen.Splash.route"""
content = content.replace(old_start, new_start)

# Add new composables before Login
login_pos = content.find("        composable(Screen.Login.route) {")
if login_pos != -1:
    new_composables = """        composable(Screen.Splash.route) {
            SplashScreen(
                viewModel = viewModel,
                onNavigateToNext = {
                    if (authState is AuthState.Success) {
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
"""
    content = content[:login_pos] + new_composables + content[login_pos:]


# Add TrustedPlaces, Permissions, About, SafeCheckin before the end
end_pos = content.rfind("    }\n}")
if end_pos != -1:
    more_composables = """        composable(Screen.TrustedPlaces.route) {
            TrustedPlacesScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(Screen.SafeCheckIn.route) {
            SafeCheckInScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(Screen.Permissions.route) {
            PermissionsScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(Screen.About.route) {
            AboutScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
"""
    content = content[:end_pos] + more_composables + content[end_pos:]

with open(filepath, "w") as f:
    f.write(content)

