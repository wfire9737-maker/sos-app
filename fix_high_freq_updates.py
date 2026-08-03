import os

filepath = "app/src/main/java/com/example/service/EmergencyService.kt"
with open(filepath, "r") as f:
    content = f.read()

target = """    private fun startHighFrequencyLocationUpdates(emergencyId: String) {
        trackingJob?.cancel()
        trackingJob = serviceScope.launch {"""

replacement = """    private fun startHighFrequencyLocationUpdates(emergencyId: String) {
        trackingJob?.cancel()
        
        val intent = android.content.Intent(context, com.example.service.LocationForegroundService::class.java).apply {
            action = "START_LOCATION_SERVICE"
        }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }

        trackingJob = serviceScope.launch {"""

content = content.replace(target, replacement)

target2 = """            countdownJob?.cancel()
            _countdown.value = null
            _activeEmergency.value = null
            trackingJob?.cancel()"""
            
replacement2 = """            countdownJob?.cancel()
            _countdown.value = null
            _activeEmergency.value = null
            trackingJob?.cancel()
            
            val intent = android.content.Intent(context, com.example.service.LocationForegroundService::class.java).apply {
                action = "STOP_LOCATION_SERVICE"
            }
            context.startService(intent)"""

content = content.replace(target2, replacement2)

with open(filepath, "w") as f:
    f.write(content)
print("Updated EmergencyService for Foreground Service")
