import re

with open("app/src/main/java/com/example/ui/navigation/NavGraph.kt", "r") as f:
    content = f.read()

import_str = "import com.example.ui.screens.AboutScreen\nimport com.example.ui.screens.DeveloperDashboardScreen"
content = content.replace("import com.example.ui.screens.AboutScreen", import_str)

route_code = """
        composable(Screen.DeveloperDashboard.route) {
            DeveloperDashboardScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.navigateUp() }
            )
        }
    }
"""

content = content.replace("        }\n    }\n\n    if (fallState == \"FALL_COUNTDOWN\") {", route_code + "\n    if (fallState == \"FALL_COUNTDOWN\") {")

with open("app/src/main/java/com/example/ui/navigation/NavGraph.kt", "w") as f:
    f.write(content)
