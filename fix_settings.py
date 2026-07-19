import os

filepath = "app/src/main/java/com/example/ui/screens/HomeScreen.kt"
with open(filepath, "r") as f:
    content = f.read()

old_nav = """        NavigationBarItem(
            icon = { Icon(Icons.Default.Person, contentDescription = null) },
            label = { Text("Profile") },
            selected = false,
            onClick = onNavigateToSettings
        )"""

new_nav = """        NavigationBarItem(
            icon = { Icon(Icons.Default.Settings, contentDescription = null) },
            label = { Text("Settings") },
            selected = false,
            onClick = onNavigateToSettings
        )"""

content = content.replace(old_nav, new_nav)

with open(filepath, "w") as f:
    f.write(content)

