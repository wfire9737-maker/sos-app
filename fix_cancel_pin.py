import os

filepath = "app/src/main/java/com/example/service/EmergencyService.kt"
with open(filepath, "r") as f:
    content = f.read()

target = """    suspend fun cancelEmergencyWithPin(pin: String, expectedPin: String, notes: String = "Cancelled with PIN"): Boolean {
        if (pin != expectedPin) {
            Log.w("EmergencyService", "PIN mismatch during emergency cancellation attempt.")
            return false
        }

        val currentModel = _activeEmergency.value ?: return false
        
        if (countdownJob?.isActive == true) {
            countdownJob?.cancel()
            _countdown.value = null
            _activeEmergency.value = null
            return true
        }"""
        
replacement = """    suspend fun cancelEmergencyWithPin(pin: String, expectedPin: String, notes: String = "Cancelled with PIN"): Boolean {
        if (countdownJob?.isActive == true) {
            countdownJob?.cancel()
            _countdown.value = null
            _activeEmergency.value = null
            return true
        }
        
        if (pin != expectedPin) {
            Log.w("EmergencyService", "PIN mismatch during emergency cancellation attempt.")
            return false
        }

        val currentModel = _activeEmergency.value ?: return false"""

if target in content:
    content = content.replace(target, replacement)
    with open(filepath, "w") as f:
        f.write(content)
    print("Fixed cancel PIN check")
else:
    print("Target not found")
