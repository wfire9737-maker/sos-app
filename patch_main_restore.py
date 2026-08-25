import re

with open('app/src/main/java/com/example/MainActivity.kt', 'r') as f:
    content = f.read()

target = """        setContent {
          // val guardianViewModel: GuardianViewModel = androidx.hilt.navigation.compose.hiltViewModel()
          val themeMode = androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf("LIGHT") }.value
          val isDarkTheme = when (themeMode) {
            "DARK" -> true
            "LIGHT" -> false
            else -> isSystemInDarkTheme()
          }
          GuardianTheme(darkTheme = isDarkTheme) {
            Surface(
              modifier = Modifier.fillMaxSize(),
              color = androidx.compose.material3.MaterialTheme.colorScheme.background
            ) {
              AppPermissionChecker()
              val context = androidx.compose.ui.platform.LocalContext.current
              val bleManager = androidx.compose.runtime.remember { com.example.ble.BleManager(context) }
              com.example.ui.screens.BleTestScreen(bleManager = bleManager)
            }
          }
        }"""

replacement = """        setContent {
          val guardianViewModel: GuardianViewModel = androidx.hilt.navigation.compose.hiltViewModel()
          val themeMode by guardianViewModel.themeMode.collectAsState()
          val isDarkTheme = when (themeMode) {
            "DARK" -> true
            "LIGHT" -> false
            else -> isSystemInDarkTheme()
          }
          GuardianTheme(darkTheme = isDarkTheme) {
            Surface(
              modifier = Modifier.fillMaxSize(),
              color = androidx.compose.material3.MaterialTheme.colorScheme.background
            ) {
              AppPermissionChecker()
              NavGraph(viewModel = guardianViewModel)
            }
          }
        }"""

content = content.replace(target, replacement)

with open('app/src/main/java/com/example/MainActivity.kt', 'w') as f:
    f.write(content)
