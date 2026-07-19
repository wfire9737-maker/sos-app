import os

filepath = "app/src/main/java/com/example/ui/navigation/NavGraph.kt"
with open(filepath, "r") as f:
    content = f.read()

old_call = """                onNavigateToAnalytics = {
                    navController.navigate(Screen.Analytics.route)
                },
                onNavigateToReports = {
                    navController.navigate(Screen.Reports.route)
                }
            )"""
new_call = """                onNavigateToAnalytics = {
                    navController.navigate(Screen.Analytics.route)
                },
                onNavigateToReports = {
                    navController.navigate(Screen.Reports.route)
                },
                onNavigateToSafeCheckIn = {
                    navController.navigate(Screen.SafeCheckIn.route)
                }
            )"""
content = content.replace(old_call, new_call)

with open(filepath, "w") as f:
    f.write(content)
