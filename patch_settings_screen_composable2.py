import re

with open("app/src/main/java/com/example/ui/screens/SettingsScreen.kt", "r") as f:
    content = f.read()

target = """    val voiceSosPhrase by viewModel.voiceSosPhrase.collectAsState()
    
    val prefs = androidx.compose.ui.platform.LocalContext.current.getSharedPreferences("smart_sos_settings", android.content.Context.MODE_PRIVATE)"""

replacement = """    val voiceSosPhrase by viewModel.voiceSosPhrase.collectAsState()
    
    val context = androidx.compose.ui.platform.LocalContext.current
    val prefs = context.getSharedPreferences("smart_sos_settings", android.content.Context.MODE_PRIVATE)"""

content = content.replace(target, replacement)

target2 = """                            nearbyPresenceInterval = nextVal
                            prefs.edit().putInt("nearby_presence_interval", nextVal).apply()
                            com.example.service.NearbyBleService.startOrStop(androidx.compose.ui.platform.LocalContext.current)
                        }"""
                        
replacement2 = """                            nearbyPresenceInterval = nextVal
                            prefs.edit().putInt("nearby_presence_interval", nextVal).apply()
                            com.example.service.NearbyBleService.startOrStop(context)
                        }"""

content = content.replace(target2, replacement2)

with open("app/src/main/java/com/example/ui/screens/SettingsScreen.kt", "w") as f:
    f.write(content)
