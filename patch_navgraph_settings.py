import re

with open("app/src/main/java/com/example/ui/navigation/NavGraph.kt", "r") as f:
    content = f.read()

target = """                onNavigateToSecurity = { navController.navigate(Screen.Security.route) },"""
replacement = """                onNavigateToNearbyDiscovery = { navController.navigate(Screen.NearbyDiscovery.route) },
                onNavigateToSecurity = { navController.navigate(Screen.Security.route) },"""

content = content.replace(target, replacement)

with open("app/src/main/java/com/example/ui/navigation/NavGraph.kt", "w") as f:
    f.write(content)
