import re

with open("app/src/main/java/com/example/ui/navigation/NavGraph.kt", "r") as f:
    content = f.read()

bad_str = """                onNavigateBack = { navController.navigateUp() }
            )
        composable(Screen.DeveloperDashboard.route) {"""

good_str = """                onNavigateBack = { navController.navigateUp() }
            )
        }
        composable(Screen.DeveloperDashboard.route) {"""

content = content.replace(bad_str, good_str)

with open("app/src/main/java/com/example/ui/navigation/NavGraph.kt", "w") as f:
    f.write(content)
