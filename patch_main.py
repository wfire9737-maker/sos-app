import re

with open('app/src/main/java/com/example/MainActivity.kt', 'r') as f:
    content = f.read()

target = """            AppPermissionChecker()
            NavGraph(viewModel = guardianViewModel)"""

replacement = """            AppPermissionChecker()
            val context = androidx.compose.ui.platform.LocalContext.current
            val bleManager = androidx.compose.runtime.remember { com.example.ble.BleManager(context) }
            com.example.ui.screens.BleTestScreen(bleManager = bleManager)"""

content = content.replace(target, replacement)

with open('app/src/main/java/com/example/MainActivity.kt', 'w') as f:
    f.write(content)
