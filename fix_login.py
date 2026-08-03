import os

filepath = "app/src/main/java/com/example/ui/navigation/NavGraph.kt"
with open(filepath, "r") as f:
    content = f.read()

target = """                onNavigateToLogin = { navController.navigateUp() }"""
replacement = """                onNavigateToLogin = { navController.navigate(Screen.Login.route) }"""

content = content.replace(target, replacement)

with open(filepath, "w") as f:
    f.write(content)

print("Restored login navigation")
