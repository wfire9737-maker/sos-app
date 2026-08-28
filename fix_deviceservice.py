import re

with open("app/src/main/java/com/example/service/DeviceService.kt", "r") as f:
    content = f.read()

replacement = """        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            if (androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.BLUETOOTH_CONNECT) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                try { com.example.service.BleForegroundService.start(context) } catch (e: Exception) {}
            }
        } else {
            try { com.example.service.BleForegroundService.start(context) } catch (e: Exception) {}
        }"""

content = content.replace("""        try {
            com.example.service.BleForegroundService.start(context)
        } catch (e: Exception) {
            Log.e("DeviceService", "Could not start BleForegroundService: ${e.message}")
        }""", replacement)

with open("app/src/main/java/com/example/service/DeviceService.kt", "w") as f:
    f.write(content)
