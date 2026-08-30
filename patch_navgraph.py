import re

with open("app/src/main/java/com/example/ui/navigation/NavGraph.kt", "r") as f:
    content = f.read()

target_import = 'import com.example.ui.screens.BleTestScreen'
replacement_import = 'import com.example.ui.screens.BleTestScreen\nimport com.example.ui.screens.NearbyDiscoveryScreen'
content = content.replace(target_import, replacement_import)

target_route = """        composable(Screen.BleTest.route) {
            com.example.ui.screens.BleTestScreen(
                bleManager = viewModel.deviceService.bleManager,
                onNavigateBack = { navController.navigateUp() }
            )
        }"""
replacement_route = """        composable(Screen.BleTest.route) {
            com.example.ui.screens.BleTestScreen(
                bleManager = viewModel.deviceService.bleManager,
                onNavigateBack = { navController.navigateUp() }
            )
        }
        composable(Screen.NearbyDiscovery.route) {
            NearbyDiscoveryScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.navigateUp() }
            )
        }"""
content = content.replace(target_route, replacement_route)

with open("app/src/main/java/com/example/ui/navigation/NavGraph.kt", "w") as f:
    f.write(content)
