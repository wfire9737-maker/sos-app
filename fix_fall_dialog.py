import os

filepath = "app/src/main/java/com/example/ui/navigation/NavGraph.kt"
with open(filepath, "r") as f:
    content = f.read()

import_target = "import com.example.ui.screens.TrustedPlacesScreen"
import_replacement = """import com.example.ui.screens.TrustedPlacesScreen
import com.example.ui.screens.FallCountdownDialog
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier"""

content = content.replace(import_target, import_replacement, 1)

navhost_target = """    NavHost(
        navController = navController,
        startDestination = Screen.Splash.route
    ) {"""

navhost_replacement = """    val fallState by viewModel.fallDetectionState.collectAsState()
    val countdown by viewModel.fallCountdown.collectAsState()

    Box(modifier = Modifier.fillMaxSize()) {
        NavHost(
            navController = navController,
            startDestination = Screen.Splash.route
        ) {"""

content = content.replace(navhost_target, navhost_replacement, 1)

end_navhost_target = """        composable(Screen.About.route) {
            AboutScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}"""

end_navhost_replacement = """        composable(Screen.About.route) {
            AboutScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }

    if (fallState == "FALL_COUNTDOWN") {
        FallCountdownDialog(
            secondsLeft = countdown,
            onCancel = { viewModel.cancelFallCountdown() }
        )
    }
    }
}"""

content = content.replace(end_navhost_target, end_navhost_replacement, 1)

with open(filepath, "w") as f:
    f.write(content)

print("Updated NavGraph with global FallCountdownDialog")
