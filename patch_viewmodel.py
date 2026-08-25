import re

with open('app/src/main/java/com/example/ui/GuardianViewModel.kt', 'r') as f:
    content = f.read()

# Replace the problematic init block part
target = """        // Voice SOS background setup
        try {
            val prefs = getApplication<Application>().getSharedPreferences("smart_sos_settings", android.content.Context.MODE_PRIVATE)
            val isVoiceEnabled = prefs.getBoolean("voice_sos_enabled", false)
            if (isVoiceEnabled) {
                val intent = android.content.Intent(getApplication(), com.example.service.VoiceSosForegroundService::class.java)
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    getApplication<Application>().startForegroundService(intent)
                } else {
                    getApplication<Application>().startService(intent)
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("GuardianViewModel", "Failed to start Voice SOS on init: ${e.message}")
        }"""

replacement = """        // Voice SOS background setup - SAFELY LOAD ONLY
        try {
            val prefs = getApplication<Application>().getSharedPreferences("smart_sos_settings", android.content.Context.MODE_PRIVATE)
            val isVoiceEnabled = prefs.getBoolean("voice_sos_enabled", false)
            if (isVoiceEnabled) {
                // Do not auto-start foreground service on launch to prevent crashes.
                // Reset to false so the user must explicitly grant permission and re-enable it.
                prefs.edit().putBoolean("voice_sos_enabled", false).apply()
                _voiceSosEnabled.value = false
            }
        } catch (e: Exception) {
            android.util.Log.e("GuardianViewModel", "Failed to load Voice SOS state safely: ${e.message}")
        }"""

content = content.replace(target, replacement)

with open('app/src/main/java/com/example/ui/GuardianViewModel.kt', 'w') as f:
    f.write(content)
