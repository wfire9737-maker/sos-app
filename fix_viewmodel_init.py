import re

with open('app/src/main/java/com/example/ui/GuardianViewModel.kt', 'r') as f:
    content = f.read()

# Look for the constructor block, likely near `class GuardianViewModel` and `init {`
init_block = '''
    init {
        // Voice SOS background setup
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
        }
'''

if "Voice SOS background setup" not in content:
    content = content.replace('init {', init_block)
    with open('app/src/main/java/com/example/ui/GuardianViewModel.kt', 'w') as f:
        f.write(content)
