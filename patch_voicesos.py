import re

with open('app/src/main/java/com/example/service/VoiceSosService.kt', 'r') as f:
    content = f.read()

# Replace the startSpeechRecognition method
start_pattern = r'    fun startSpeechRecognition\(context: Context\) \{.*?(?=    fun stopSpeechRecognition\(\) \{)'
# We'll rewrite the entire block carefully

