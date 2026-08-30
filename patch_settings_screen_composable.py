import re

with open("app/src/main/java/com/example/ui/screens/SettingsScreen.kt", "r") as f:
    content = f.read()

target = """                            nearbyPresenceInterval = nextVal
                            prefs.edit().putInt("nearby_presence_interval", nextVal).apply()
                            com.example.service.NearbyBleService.startOrStop(androidx.compose.ui.platform.LocalContext.current)
                        }"""
replacement = """                            nearbyPresenceInterval = nextVal
                            prefs.edit().putInt("nearby_presence_interval", nextVal).apply()
                        }
                    )
                    
                    LaunchedEffect(nearbyPresenceInterval) {
                        com.example.service.NearbyBleService.startOrStop(context)
                    }"""

# I need to use the context from outside the onClick lambda. Wait, I can just use the `context` val that should already exist, or get it first.
# Let's check if context exists in SettingsScreen

