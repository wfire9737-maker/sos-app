import os

filepath = "app/src/main/java/com/example/ui/navigation/NavGraph.kt"
with open(filepath, "r") as f:
    content = f.read()

# I see Map isn't actually being used correctly on the HomeScreen in the NavGraph.
old_nav = """                onNavigateToSafeCheckIn = {
                    navController.navigate(Screen.SafeCheckIn.route)
                },
                onNavigateToMap = {
                    navController.navigate(Screen.Map.route)
                }
            )"""
new_nav = """                onNavigateToSafeCheckIn = {
                    navController.navigate(Screen.SafeCheckIn.route)
                },
                onNavigateToMap = {
                    navController.navigate(Screen.Map.route)
                }
            )"""
            
if old_nav in content:
    print("Found map nav")

