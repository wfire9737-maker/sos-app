import os

filepath = "app/src/main/java/com/example/ui/navigation/NavGraph.kt"
with open(filepath, "r") as f:
    content = f.read()

content = content.replace("onNavigateToLogin = { navController.navigate(Screen.Login.route) }", "onNavigateToLogin = { navController.popBackStack() }")

with open(filepath, "w") as f:
    f.write(content)
