import os

filepath = "app/src/main/java/com/example/ui/screens/SettingsScreen.kt"
with open(filepath, "r") as f:
    content = f.read()

old_theme = """                SettingsSection(title = "Preferences") {
                    SettingsItem(
                        icon = Icons.Default.Notifications,
                        title = "Notifications","""
new_theme = """                SettingsSection(title = "Preferences") {
                    SettingsItem(
                        icon = Icons.Default.DarkMode,
                        title = "Dark Theme",
                        subtitle = "Toggle dark mode",
                        onClick = { /* TODO: Implement theme switching via ViewModel */ }
                    )
                    SettingsItem(
                        icon = Icons.Default.Notifications,
                        title = "Notifications","""
content = content.replace(old_theme, new_theme)

with open(filepath, "w") as f:
    f.write(content)
