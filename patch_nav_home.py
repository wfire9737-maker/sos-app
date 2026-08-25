import re

with open('app/src/main/java/com/example/ui/navigation/NavGraph.kt', 'r') as f:
    content = f.read()

target = """                onNavigateToMap = {
                    navController.navigate(Screen.Map.route)
                }"""

replacement = """                onNavigateToMap = {
                    navController.navigate(Screen.Map.route)
                },
                onNavigateToBleTest = {
                    navController.navigate(Screen.BleTest.route)
                }"""

content = content.replace(target, replacement)

with open('app/src/main/java/com/example/ui/navigation/NavGraph.kt', 'w') as f:
    f.write(content)
