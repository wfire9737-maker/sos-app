import re

with open("app/src/main/java/com/example/ui/GuardianViewModel.kt", "r") as f:
    content = f.read()

init_block = """init {
        voiceSosService.onVoiceSosTriggered = { phrase, confidence ->
            triggerVoiceSOS(phrase, confidence)
        }
        
        // Start service if enabled on boot/init
        if (_voiceSosEnabled.value) {
            val intent = android.content.Intent(getApplication(), com.example.service.VoiceSosForegroundService::class.java)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                getApplication<android.app.Application>().startForegroundService(intent)
            } else {
                getApplication<android.app.Application>().startService(intent)
            }
        }
    }

    private val _uiEvents"""

content = re.sub(r'private val _uiEvents', init_block, content)

with open("app/src/main/java/com/example/ui/GuardianViewModel.kt", "w") as f:
    f.write(content)
