import re

with open("app/src/main/java/com/example/ui/navigation/NavGraph.kt", "r") as f:
    content = f.read()

target = """                onNavigateToNearbyDiscovery = { navController.navigate(Screen.NearbyDiscovery.route) },
                onNavigateToSecurity = { navController.navigate(Screen.Security.route) },"""
replacement = """                onNavigateToSecurity = { navController.navigate(Screen.Security.route) },"""
content = content.replace(target, replacement)

target_home = """                onNavigateToMap = {
                    navController.navigate(Screen.Map.route)
                },
                onNavigateToBleTest = {
                    navController.navigate(Screen.BleTest.route)
                }"""
replacement_home = """                onNavigateToMap = {
                    navController.navigate(Screen.Map.route)
                },
                onNavigateToBleTest = {
                    navController.navigate(Screen.BleTest.route)
                },
                onNavigateToNearbyDiscovery = {
                    navController.navigate(Screen.NearbyDiscovery.route)
                }"""
content = content.replace(target_home, replacement_home)

with open("app/src/main/java/com/example/ui/navigation/NavGraph.kt", "w") as f:
    f.write(content)
