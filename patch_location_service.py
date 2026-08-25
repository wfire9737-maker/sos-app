import re

with open('app/src/main/java/com/example/service/LocationForegroundService.kt', 'r') as f:
    content = f.read()

replacement = """        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(1001, notification, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
            } else {
                startForeground(1001, notification)
            }
        } catch (e: Exception) {
            android.util.Log.e("LocationFgService", "Failed to start foreground service: ${e.message}")
        }"""

target = """        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(1001, notification, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
        } else {
            startForeground(1001, notification)
        }"""

content = content.replace(target, replacement)

with open('app/src/main/java/com/example/service/LocationForegroundService.kt', 'w') as f:
    f.write(content)
