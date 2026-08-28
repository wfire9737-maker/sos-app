import re

with open("app/src/main/java/com/example/ui/GuardianViewModel.kt", "r") as f:
    content = f.read()

# Fix private val voiceSosService to val
content = content.replace("private val voiceSosService", "val voiceSosService")

# Fix MutableStateFlow types
replacements = [
    (r'_voiceSosEnabled = MutableStateFlow<Any\?>\(null\)', r'_voiceSosEnabled = MutableStateFlow(false)'),
    (r'_isSirenPlaying = MutableStateFlow<Any\?>\(null\)', r'_isSirenPlaying = MutableStateFlow(false)'),
    (r'_voiceCommandConfirmation = MutableStateFlow<Any\?>\(null\)', r'_voiceCommandConfirmation = MutableStateFlow<String?>(null)'),
    (r'_sosWorkflowState = MutableStateFlow<Any\?>\(null\)', r'_sosWorkflowState = MutableStateFlow(SosWorkflowState.IDLE)'),
    (r'_emergencySession = MutableStateFlow<Any\?>\(null\)', r'_emergencySession = MutableStateFlow<EmergencySession?>(null)'),
    (r'_themeMode = MutableStateFlow<Any\?>\(null\)', r'_themeMode = MutableStateFlow("SYSTEM")'),
    (r'_language = MutableStateFlow<Any\?>\(null\)', r'_language = MutableStateFlow("en")'),
    (r'_criticalAlarmsEnabled = MutableStateFlow<Any\?>\(null\)', r'_criticalAlarmsEnabled = MutableStateFlow(true)'),
    (r'_arrivalAlertsEnabled = MutableStateFlow<Any\?>\(null\)', r'_arrivalAlertsEnabled = MutableStateFlow(true)'),
    (r'_deviceStatusNotificationsEnabled = MutableStateFlow<Any\?>\(null\)', r'_deviceStatusNotificationsEnabled = MutableStateFlow(true)'),
    (r'_locationSharingInterval = MutableStateFlow<Any\?>\(null\)', r'_locationSharingInterval = MutableStateFlow("10s")'),
    (r'_backgroundLocationEnabled = MutableStateFlow<Any\?>\(null\)', r'_backgroundLocationEnabled = MutableStateFlow(true)'),
    (r'_telemetrySharingEnabled = MutableStateFlow<Any\?>\(null\)', r'_telemetrySharingEnabled = MutableStateFlow(true)'),
    (r'_biometricEnabled = MutableStateFlow<Any\?>\(null\)', r'_biometricEnabled = MutableStateFlow(false)'),
    (r'_appLockPinEnabled = MutableStateFlow<Any\?>\(null\)', r'_appLockPinEnabled = MutableStateFlow(false)'),
    (r'_appLockPin = MutableStateFlow<Any\?>\(null\)', r'_appLockPin = MutableStateFlow("")'),
    (r'_emergencyPin = MutableStateFlow<Any\?>\(null\)', r'_emergencyPin = MutableStateFlow("")'),
    (r'_isBackupRunning = MutableStateFlow<Any\?>\(null\)', r'_isBackupRunning = MutableStateFlow(false)'),
    (r'_lastBackupTime = MutableStateFlow<Any\?>\(null\)', r'_lastBackupTime = MutableStateFlow(0L)'),
    (r'_voiceSosPhrase = MutableStateFlow<Any\?>\(null\)', r'_voiceSosPhrase = MutableStateFlow("Emergency SOS")'),
]

for t, r in replacements:
    content = re.sub(t, r, content)

# Fix stateIn types
content = content.replace("stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null as Any?)", "stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)")

with open("app/src/main/java/com/example/ui/GuardianViewModel.kt", "w") as f:
    f.write(content)

