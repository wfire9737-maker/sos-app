import re

with open("app/src/main/java/com/example/ui/GuardianViewModel.kt", "r") as f:
    content = f.read()

# 1. Update setVoiceSosEnabled
replacement_setVoiceSosEnabled = """fun setVoiceSosEnabled(enabled: Boolean) {
        _voiceSosEnabled.value = enabled
        val smartSosPrefs = getApplication<android.app.Application>().getSharedPreferences("smart_sos_settings", android.content.Context.MODE_PRIVATE)
        smartSosPrefs.edit().putBoolean("voice_sos_enabled", enabled).apply()

        if (enabled) {
            val intent = android.content.Intent(getApplication(), com.example.service.VoiceSosForegroundService::class.java)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                getApplication<android.app.Application>().startForegroundService(intent)
            } else {
                getApplication<android.app.Application>().startService(intent)
            }
        } else {
            val intent = android.content.Intent(getApplication(), com.example.service.VoiceSosForegroundService::class.java).apply {
                action = "STOP"
            }
            getApplication<android.app.Application>().startService(intent)
        }
    }"""
content = re.sub(r'fun setVoiceSosEnabled\(enabled: Boolean\) \{ \}', replacement_setVoiceSosEnabled, content)

# 2. Add initialization of _voiceSosEnabled in init block? Or just do it in the flow initialization.
replacement_init_flow = """private val _voiceSosEnabled = MutableStateFlow(
        getApplication<android.app.Application>().getSharedPreferences("smart_sos_settings", android.content.Context.MODE_PRIVATE)
            .getBoolean("voice_sos_enabled", false)
    )"""
content = re.sub(r'private val _voiceSosEnabled = MutableStateFlow\(false\)', replacement_init_flow, content)

# 3. Update triggerVoiceSOS
replacement_triggerVoiceSOS = """fun triggerVoiceSOS(matchedPhrase: String, confidence: Int) {
        emergencyProvider.triggerEmergency(
            triggerSource = "VOICE_SOS",
            lat = null,
            lng = null
        )
    }"""
content = re.sub(r'fun triggerVoiceSOS\(matchedPhrase: String, confidence: Int\) \{ \}', replacement_triggerVoiceSOS, content)

with open("app/src/main/java/com/example/ui/GuardianViewModel.kt", "w") as f:
    f.write(content)
