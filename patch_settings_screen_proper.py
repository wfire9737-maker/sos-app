import re

with open("app/src/main/java/com/example/ui/screens/SettingsScreen.kt", "r") as f:
    content = f.read()

# I need to add nearbyPresenceInterval to the top
var_to_add = """
    val voiceSosPhrase by viewModel.voiceSosPhrase.collectAsState()
    
    val prefs = androidx.compose.ui.platform.LocalContext.current.getSharedPreferences("smart_sos_settings", android.content.Context.MODE_PRIVATE)
    var nearbyPresenceInterval by remember { mutableStateOf(prefs.getInt("nearby_presence_interval", 0)) }
"""
content = content.replace("    val voiceSosPhrase by viewModel.voiceSosPhrase.collectAsState()", var_to_add)

# Now add the setting itself under "Account & Security" or create a new section for "Nearby BLE"
new_section = """
            item {
                SettingsSection(title = "Nearby Emergency Presence") {
                    val presenceOptions = listOf(0, 5, 10, 30, 60)
                    val presenceLabels = mapOf(0 to "Off", 5 to "5 seconds", 10 to "10 seconds", 30 to "30 seconds", 60 to "60 seconds")
                    
                    SettingsItem(
                        icon = Icons.Default.WifiTethering,
                        title = "Nearby Presence (BLE)",
                        subtitle = "Frequency: " + (presenceLabels[nearbyPresenceInterval] ?: "Off"),
                        onClick = {
                            val currentIndex = presenceOptions.indexOf(nearbyPresenceInterval)
                            val nextIndex = (currentIndex + 1) % presenceOptions.size
                            val nextVal = presenceOptions[nextIndex]
                            nearbyPresenceInterval = nextVal
                            prefs.edit().putInt("nearby_presence_interval", nextVal).apply()
                        }
                    )
                }
            }
"""

if "Nearby Emergency Presence" not in content:
    content = content.replace("            item {\n                SettingsSection(title = \"Voice SOS & Emergency Phrases\") {", new_section + "\n            item {\n                SettingsSection(title = \"Voice SOS & Emergency Phrases\") {")

with open("app/src/main/java/com/example/ui/screens/SettingsScreen.kt", "w") as f:
    f.write(content)

