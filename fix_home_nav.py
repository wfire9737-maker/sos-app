import os

filepath = "app/src/main/java/com/example/ui/screens/HomeScreen.kt"
with open(filepath, "r") as f:
    content = f.read()

old_call = """                onNavigateToSettings = onNavigateToProfile"""
new_call = """                onNavigateToSettings = onNavigateToSettings"""
content = content.replace(old_call, new_call)

with open(filepath, "w") as f:
    f.write(content)

