import os

filepath = "app/src/main/java/com/example/service/EmergencyService.kt"
with open(filepath, "r") as f:
    content = f.read()

target = """    private fun closeActiveSession() {
        trackingJob?.cancel()
        trackingJob = null
        _activeEmergency.value = null
    }"""

replacement = """    private fun closeActiveSession() {
        trackingJob?.cancel()
        trackingJob = null
        _activeEmergency.value = null
        
        val intent = android.content.Intent(context, com.example.service.LocationForegroundService::class.java).apply {
            action = "STOP_LOCATION_SERVICE"
        }
        context.startService(intent)
    }"""

content = content.replace(target, replacement)
with open(filepath, "w") as f:
    f.write(content)
print("Updated closeActiveSession")
