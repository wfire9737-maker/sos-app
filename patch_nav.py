import re

with open('app/src/main/java/com/example/ui/navigation/NavGraph.kt', 'r') as f:
    content = f.read()

target = """        composable(Screen.DeveloperDashboard.route) {
            DeveloperDashboardScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.navigateUp() }
            )
        }"""

replacement = """        composable(Screen.DeveloperDashboard.route) {
            DeveloperDashboardScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.navigateUp() }
            )
        }
        composable(Screen.BleTest.route) {
            com.example.ui.screens.BleTestScreen(
                bleManager = viewModel.deviceService.bleManager
            )
        }"""

content = content.replace(target, replacement)

with open('app/src/main/java/com/example/ui/navigation/NavGraph.kt', 'w') as f:
    f.write(content)
