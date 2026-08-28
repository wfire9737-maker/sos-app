import re

with open("app/src/main/java/com/example/ui/GuardianViewModel.kt", "r") as f:
    content = f.read()

# I will find the exact init block and replace it
target = """init {
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
    }"""

replacement = """init {
        // Start service if enabled on boot/init
        if (_voiceSosEnabled.value) {
            val intent = android.content.Intent(getApplication(), com.example.service.VoiceSosForegroundService::class.java)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                getApplication<android.app.Application>().startForegroundService(intent)
            } else {
                getApplication<android.app.Application>().startService(intent)
            }
        }
    }"""

if target in content:
    content = content.replace(target, replacement)
    with open("app/src/main/java/com/example/ui/GuardianViewModel.kt", "w") as f:
        f.write(content)
        print("Success")
else:
    print("Not found")
