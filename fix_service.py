import re

with open('app/src/main/java/com/example/service/VoiceSosForegroundService.kt', 'r') as f:
    content = f.read()

# Replace icon
content = content.replace('android.R.drawable.ic_btn_speak_now', 'com.example.R.mipmap.ic_launcher')

# Wrap startForeground in try-catch
old_start = """        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
        voiceSosService.isContinuousMode = true
        voiceSosService.startSpeechRecognition(this)
        return START_STICKY"""

new_start = """        try {
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

content = content.replace(old_start, new_start)

with open('app/src/main/java/com/example/service/VoiceSosForegroundService.kt', 'w') as f:
    f.write(content)
