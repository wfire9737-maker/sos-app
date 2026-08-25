import re

with open('app/src/main/java/com/example/service/VoiceSosForegroundService.kt', 'r') as f:
    content = f.read()

replacement = """        try {
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

target = """        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
        voiceSosService.isContinuousMode = true
        voiceSosService.startSpeechRecognition(this)
        return START_STICKY"""

content = content.replace(target, replacement)

with open('app/src/main/java/com/example/service/VoiceSosForegroundService.kt', 'w') as f:
    f.write(content)
