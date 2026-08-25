import re

with open('app/src/main/java/com/example/MainActivity.kt', 'r') as f:
    content = f.read()

target = """              AppPermissionChecker()
              NavGraph(viewModel = guardianViewModel)"""

replacement = """              AppPermissionChecker()
              val context = androidx.compose.ui.platform.LocalContext.current
              val bleManager = androidx.compose.runtime.remember { com.example.ble.BleManager(context) }
              com.example.ui.screens.BleTestScreen(bleManager = bleManager)"""

content = content.replace(target, replacement)

target2 = """val guardianViewModel: GuardianViewModel = androidx.hilt.navigation.compose.hiltViewModel()"""
replacement2 = """// val guardianViewModel: GuardianViewModel = androidx.hilt.navigation.compose.hiltViewModel()"""
content = content.replace(target2, replacement2)

target3 = """val themeMode by guardianViewModel.themeMode.collectAsState()"""
replacement3 = """val themeMode = androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf("LIGHT") }.value"""
content = content.replace(target3, replacement3)

with open('app/src/main/java/com/example/MainActivity.kt', 'w') as f:
    f.write(content)
