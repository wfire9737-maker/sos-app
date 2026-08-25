import re

with open('app/src/main/java/com/example/ui/navigation/NavGraph.kt', 'r') as f:
    content = f.read()

target = """        composable(Screen.BleTest.route) {
            com.example.ui.screens.BleTestScreen(
                bleManager = viewModel.deviceService.bleManager
            )
        }"""

replacement = """        composable(Screen.BleTest.route) {
            com.example.ui.screens.BleTestScreen(
                bleManager = viewModel.deviceService.bleManager,
                onNavigateBack = { navController.navigateUp() }
            )
        }"""

content = content.replace(target, replacement)

with open('app/src/main/java/com/example/ui/navigation/NavGraph.kt', 'w') as f:
    f.write(content)
