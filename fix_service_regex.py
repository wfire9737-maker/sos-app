import re

with open('app/src/main/java/com/example/service/VoiceSosForegroundService.kt', 'r') as f:
    content = f.read()

# Replace icon
content = re.sub(r'android\.R\.drawable\.ic_btn_speak_now', 'com.example.R.mipmap.ic_launcher', content)

# Replace startForeground block
pattern = r'(\s*if\s*\(Build\.VERSION\.SDK_INT\s*>=\s*Build\.VERSION_CODES\.R\)\s*\{.*?return\s*START_STICKY)'
replacement = """
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE)
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
            voiceSosService.isContinuousMode = true
            voiceSosService.startSpeechRecognition(this)
        } catch (e: Exception) {
            Log.e("VoiceSosFgService", "Failed to start foreground service: ${e.message}")
            stopSelf()
            return START_NOT_STICKY
        }
        return START_STICKY"""

content = re.sub(pattern, replacement, content, flags=re.DOTALL)

with open('app/src/main/java/com/example/service/VoiceSosForegroundService.kt', 'w') as f:
    f.write(content)
