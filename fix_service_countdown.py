import os
import re

filepath = "app/src/main/java/com/example/service/EmergencyService.kt"
with open(filepath, "r") as f:
    content = f.read()

target = """    private var trackingJob: Job? = null

    private val _activeEmergency = MutableStateFlow<EmergencyModel?>(null)
    val activeEmergency: StateFlow<EmergencyModel?> = _activeEmergency.asStateFlow()

    fun isEmergencyActive(): Boolean = _activeEmergency.value != null"""

replacement = """    private var trackingJob: Job? = null
    private var countdownJob: Job? = null

    private val _activeEmergency = MutableStateFlow<EmergencyModel?>(null)
    val activeEmergency: StateFlow<EmergencyModel?> = _activeEmergency.asStateFlow()
    
    private val _countdown = MutableStateFlow<Int?>(null)
    val countdown: StateFlow<Int?> = _countdown.asStateFlow()

    fun isEmergencyActive(): Boolean = _activeEmergency.value != null || countdownJob?.isActive == true"""

if target in content:
    content = content.replace(target, replacement)
    
    # We also need to change startEmergency
    # find startEmergency body
    start_target = """        // Prevent duplicate SOS sessions
        _activeEmergency.value?.let {
            Log.w("EmergencyService", "An active emergency session is already running: ${it.emergencyId}")
            return it
        }"""
    start_replacement = """        // Prevent duplicate SOS sessions
        _activeEmergency.value?.let {
            Log.w("EmergencyService", "An active emergency session is already running: ${it.emergencyId}")
            return it
        }
        
        if (countdownJob?.isActive == true) {
            Log.w("EmergencyService", "Countdown already running.")
            return EmergencyModel(emergencyId = "PENDING", userId = userId, userName = userName, userPhone = userPhone, startTimeMs = System.currentTimeMillis(), latitude = 0.0, longitude = 0.0, status = "COUNTDOWN", triggerType = triggerType, aiConfidenceScore = 100, contactsNotified = listOf(), responderStatus = "COUNTDOWN", deviceId = deviceId)
        }"""
    content = content.replace(start_target, start_replacement)

    with open(filepath, "w") as f:
        f.write(content)
    print("Added countdown flow to EmergencyService")
else:
    print("Target not found")
