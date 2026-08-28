import re

with open("app/src/main/java/com/example/service/VoiceSosService.kt", "r") as f:
    content = f.read()

# Just empty startMicLevelMonitor() block so it does nothing. 
# Or we can let it set back to 30f if NOT listening.
replacement = """    private fun startMicLevelMonitor() {
        micPollerJob?.cancel()
        micPollerJob = serviceScope.launch {
            while (isActive) {
                if (!_isListening.value && !_isSpeechRecognizerActive.value) {
                    _micDecibels.value = 30f
                }
                delay(120)
            }
        }
    }"""
content = re.sub(r'private fun startMicLevelMonitor\(\) \{.*?\n    \}', replacement, content, flags=re.DOTALL)

with open("app/src/main/java/com/example/service/VoiceSosService.kt", "w") as f:
    f.write(content)
