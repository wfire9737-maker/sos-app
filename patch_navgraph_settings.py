import re

with open("app/src/main/java/com/example/ui/navigation/NavGraph.kt", "r") as f:
    content = f.read()

content = content.replace(
    "onNavigateToAbout = { navController.navigate(Screen.About.route) }",
    "onNavigateToAbout = { navController.navigate(Screen.About.route) },\n                onNavigateToDeveloperDashboard = { navController.navigate(Screen.DeveloperDashboard.route) }"
)

with open("app/src/main/java/com/example/ui/navigation/NavGraph.kt", "w") as f:
    f.write(content)
