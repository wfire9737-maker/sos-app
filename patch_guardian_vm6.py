import re

with open('app/src/main/java/com/example/ui/GuardianViewModel.kt', 'r') as f:
    content = f.read()

bad_block = """    fun setVoiceSosEnabled(enabled: Boolean) {
        _voiceSosEnabled.value = enabled
        try {
            getApplication<Application>().getSharedPreferences("smart_sos_settings", android.content.Context.MODE_PRIVATE)
                .edit()
                .putBoolean("voice_sos_enabled", enabled)
                .apply()
            
            val intent = android.content.Intent(getApplication(), com.example.service.VoiceSosForegroundService::class.java)
            if (enabled) {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    getApplication<Application>().startForegroundService(intent)
                } else {
                    getApplication<Application>().startService(intent)
                }
            } else {
                intent.action = "STOP"
                getApplication<Application>().startService(intent)
            }
        } catch (e: Exception) {
            android.util.Log.e("GuardianViewModel", "Failed to save/start voice_sos_enabled: ${e.message}")
        }
    }"""

good_block = """    fun setVoiceSosEnabled(enabled: Boolean) {
        _voiceSosEnabled.value = enabled
        databaseService.saveUserSetting("voice_sos_enabled", enabled)
        
        try {
            val intent = android.content.Intent(getApplication(), com.example.service.VoiceSosForegroundService::class.java)
            if (enabled) {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    getApplication<Application>().startForegroundService(intent)
                } else {
                    getApplication<Application>().startService(intent)
                }
            } else {
                intent.action = "STOP"
                getApplication<Application>().startService(intent)
            }
        } catch (e: Exception) {
            android.util.Log.e("GuardianViewModel", "Failed to start/stop voice_sos_enabled: ${e.message}")
        }
    }"""

content = content.replace(bad_block, good_block)

with open('app/src/main/java/com/example/ui/GuardianViewModel.kt', 'w') as f:
    f.write(content)
