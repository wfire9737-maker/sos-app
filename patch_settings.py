import re

with open("app/src/main/java/com/example/ui/screens/SettingsScreen.kt", "r") as f:
    content = f.read()

content = content.replace(
    "onNavigateToAbout: () -> Unit = {}",
    "onNavigateToAbout: () -> Unit = {},\n    onNavigateToDeveloperDashboard: () -> Unit = {}"
)

new_item = """
                    if (developerModeEnabled) {
                        SettingsItem(
                            icon = Icons.Default.Info,
                            title = "Developer Dashboard",
                            subtitle = "View advanced application status",
                            onClick = onNavigateToDeveloperDashboard
                        )
                    }
                    SettingsItem(
                        icon = Icons.Default.Info,
"""
content = content.replace('                    SettingsItem(\n                        icon = Icons.Default.Info,\n                        title = "About Smart SOS",', new_item + '                        title = "About Smart SOS",')

with open("app/src/main/java/com/example/ui/screens/SettingsScreen.kt", "w") as f:
    f.write(content)
