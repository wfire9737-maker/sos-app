import re

with open("app/src/main/java/com/example/service/EmergencyProvider.kt", "r") as f:
    content = f.read()

# Add a timestamp variable inside the class
if "private var lastUpdateNotificationTime: Long = 0L" not in content:
    content = content.replace("private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())",
                              "private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())\n    private var lastUpdateNotificationTime: Long = 0L")

# Replace the block that handles active emergency updates
old_update_block = """            if (isEmergencyInProgress()) {
                emergencyService.activeEmergency.value?.let { model ->
                    emergencyService.notifyEmergencyContacts(model, isUpdate = true)
                }
                return@launch
            }"""

new_update_block = """            if (isEmergencyInProgress()) {
                val now = System.currentTimeMillis()
                if (now - lastUpdateNotificationTime > 30000) { // Limit updates to once every 30 seconds
                    lastUpdateNotificationTime = now
                    emergencyService.activeEmergency.value?.let { model ->
                        emergencyService.notifyEmergencyContacts(model, isUpdate = true)
                    }
                } else {
                    android.util.Log.d("Emergency", "EMERGENCY: Duplicate trigger ignored (rate limited)")
                }
                return@launch
            }"""

content = content.replace(old_update_block, new_update_block)

with open("app/src/main/java/com/example/service/EmergencyProvider.kt", "w") as f:
    f.write(content)
