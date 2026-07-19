import os

filepath = "app/src/main/java/com/example/ui/navigation/NavGraph.kt"
with open(filepath, "r") as f:
    content = f.read()

old_call = """                onNavigateToSafeCheckIn = {
                    navController.navigate(Screen.SafeCheckIn.route)
                }
            )"""
new_call = """                onNavigateToSafeCheckIn = {
                    navController.navigate(Screen.SafeCheckIn.route)
                },
                onNavigateToMap = {
                    navController.navigate(Screen.Map.route)
                }
            )"""
content = content.replace(old_call, new_call)

with open(filepath, "w") as f:
    f.write(content)

