import re

with open("app/src/main/java/com/example/ui/screens/AboutScreen.kt", "r") as f:
    content = f.read()

import_str = """
import androidx.compose.foundation.clickable
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import kotlinx.coroutines.launch
import com.example.ui.GuardianViewModel
"""
content = content.replace("import com.example.ui.GuardianViewModel", import_str)

new_func = """
fun AboutScreen(viewModel: GuardianViewModel, onNavigateBack: () -> Unit) {
    val developerModeEnabled by viewModel.developerModeEnabled.collectAsState()
    var clickCount by remember { mutableStateOf(0) }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("About Smart SOS", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier.fillMaxSize().padding(paddingValues).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("Smart SOS", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Text(
                "Version 1.0.0", 
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.clickable {
                    if (!developerModeEnabled) {
                        clickCount++
                        if (clickCount >= 7) {
                            viewModel.setDeveloperModeEnabled(true)
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("Developer Mode Activated. Warning: Intended for testing.")
                            }
                            clickCount = 0
                        } else if (clickCount >= 3) {
                            val remaining = 7 - clickCount
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("You are $remaining steps away from being a developer.")
                            }
                        }
                    } else {
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("Developer Mode is already enabled.")
                        }
                    }
                }
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text("Your personal safety companion.", style = MaterialTheme.typography.bodyMedium)
        }
    }
}
"""

content = re.sub(r'fun AboutScreen.*', new_func, content, flags=re.DOTALL)
with open("app/src/main/java/com/example/ui/screens/AboutScreen.kt", "w") as f:
    f.write(content)

