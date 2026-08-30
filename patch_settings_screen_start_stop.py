import re

with open("app/src/main/java/com/example/ui/screens/SettingsScreen.kt", "r") as f:
    content = f.read()

target = """                            nearbyPresenceInterval = nextVal
                            prefs.edit().putInt("nearby_presence_interval", nextVal).apply()
                        }
                    )"""
replacement = """                            nearbyPresenceInterval = nextVal
                            prefs.edit().putInt("nearby_presence_interval", nextVal).apply()
                            com.example.service.NearbyBleService.startOrStop(androidx.compose.ui.platform.LocalContext.current)
                        }
                    )"""

content = content.replace(target, replacement)

with open("app/src/main/java/com/example/ui/screens/SettingsScreen.kt", "w") as f:
    f.write(content)
