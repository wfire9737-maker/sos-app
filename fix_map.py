import os

filepath = "app/src/main/java/com/example/ui/screens/HomeScreen.kt"
with open(filepath, "r") as f:
    content = f.read()

old_nav = """fun HomeBottomNav(onNavigateToMap: () -> Unit = {}, onNavigateToContacts: () -> Unit, onNavigateToHistory: () -> Unit, onNavigateToSettings: () -> Unit) {"""
new_nav = """fun HomeBottomNav(onNavigateToMap: () -> Unit, onNavigateToContacts: () -> Unit, onNavigateToHistory: () -> Unit, onNavigateToSettings: () -> Unit) {"""

content = content.replace(old_nav, new_nav)

with open(filepath, "w") as f:
    f.write(content)

