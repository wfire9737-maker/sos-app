import re

with open('app/src/main/java/com/example/service/VoiceSosService.kt', 'r') as f:
    content = f.read()

# Replace startSpeechRecognition context handling
start_pattern = r'if \(speechRecognizer == null\) \{\s*speechRecognizer = SpeechRecognizer\.createSpeechRecognizer\(context\)\s*\}'
start_replacement = '''
                // Always recreate to ensure we use the Foreground Service context
                speechRecognizer?.destroy()
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
'''
content = re.sub(start_pattern, start_replacement.strip(), content)

# Remove the cleanup stopSpeechRecognition so it survives ViewModel destruction if it's continuous
cleanup_pattern = r'fun cleanup\(\) \{\s*stopSpeechRecognition\(\)\s*micPollerJob\?\.cancel\(\)\s*\}'
cleanup_replacement = '''
    fun cleanup() {
        if (!isContinuousMode) {
            stopSpeechRecognition()
        }
        micPollerJob?.cancel()
    }
'''
content = re.sub(cleanup_pattern, cleanup_replacement.strip(), content)

with open('app/src/main/java/com/example/service/VoiceSosService.kt', 'w') as f:
    f.write(content)
