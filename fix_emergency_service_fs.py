import os

filepath = "app/src/main/java/com/example/service/EmergencyService.kt"
with open(filepath, "r") as f:
    content = f.read()

# Start foreground service in startEmergencyTracking
target_start = """    private fun startEmergencyTracking() {
        trackingJob?.cancel()
        trackingJob = serviceScope.launch {
            while (true) {
                delay(10000) // 10s intervals for live tracking
                _activeEmergency.value?.let { current ->"""

replacement_start = """    private fun startEmergencyTracking() {
        val intent = android.content.Intent(context, LocationForegroundService::class.java).apply {
            action = "START_LOCATION_SERVICE"
        }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }

        trackingJob?.cancel()
        trackingJob = serviceScope.launch {
            while (true) {
                delay(10000) // 10s intervals for live tracking
                _activeEmergency.value?.let { current ->"""

content = content.replace(target_start, replacement_start)

# Stop foreground service in cancelEmergencyWithPin
target_stop = """            countdownJob?.cancel()
            _countdown.value = null
            _activeEmergency.value = null
            trackingJob?.cancel()"""

replacement_stop = """            countdownJob?.cancel()
            _countdown.value = null
            _activeEmergency.value = null
            trackingJob?.cancel()
            
            val intent = android.content.Intent(context, LocationForegroundService::class.java).apply {
                action = "STOP_LOCATION_SERVICE"
            }
            context.startService(intent)"""

content = content.replace(target_stop, replacement_stop)

if "import android.content.Intent" not in content:
    content = content.replace("import android.content.Context", "import android.content.Context\nimport android.content.Intent\nimport com.example.service.LocationForegroundService")

with open(filepath, "w") as f:
    f.write(content)
print("Added ForegroundService to EmergencyService")
