import re

with open('app/src/main/java/com/example/ui/screens/SettingsScreen.kt', 'r') as f:
    content = f.read()

# The old Voice SOS item block looks like this:
#                    SettingsItem(
#                        icon = Icons.Default.Mic,
#                        title = "Voice SOS",
#                        subtitle = "Configure voice activation",
#                        onClick = onNavigateToVoiceSos
#                    )
# We want to remove it, but keep the one we just added which has:
#                    SettingsSwitchItem(
#                        icon = Icons.Default.Mic,
#                        title = "Voice SOS",

content = re.sub(r'\s*SettingsItem\(\s*icon = Icons\.Default\.Mic,\s*title = "Voice SOS",\s*subtitle = "Configure voice activation",\s*onClick = onNavigateToVoiceSos\s*\)', '', content)

with open('app/src/main/java/com/example/ui/screens/SettingsScreen.kt', 'w') as f:
    f.write(content)
