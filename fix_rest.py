import re

with open("app/src/main/java/com/example/ui/GuardianViewModel.kt", "r") as f:
    lines = f.read().split('\n')

missing = [
    ('criticalAlarmsEnabled', 'private val _criticalAlarmsEnabled = MutableStateFlow(true)'),
    ('arrivalAlertsEnabled', 'private val _arrivalAlertsEnabled = MutableStateFlow(true)'),
    ('deviceStatusNotificationsEnabled', 'private val _deviceStatusNotificationsEnabled = MutableStateFlow(true)'),
    ('locationSharingInterval', 'private val _locationSharingInterval = MutableStateFlow("10s")'),
    ('backgroundLocationEnabled', 'private val _backgroundLocationEnabled = MutableStateFlow(true)'),
    ('telemetrySharingEnabled', 'private val _telemetrySharingEnabled = MutableStateFlow(true)'),
    ('biometricEnabled', 'private val _biometricEnabled = MutableStateFlow(false)'),
    ('appLockPinEnabled', 'private val _appLockPinEnabled = MutableStateFlow(false)'),
    ('appLockPin', 'private val _appLockPin = MutableStateFlow("")'),
    ('emergencyPin', 'private val _emergencyPin = MutableStateFlow("")'),
    ('isBackupRunning', 'private val _isBackupRunning = MutableStateFlow(false)'),
    ('lastBackupTime', 'private val _lastBackupTime = MutableStateFlow(0L)'),
]

new_lines = []
for line in lines:
    new_lines.append(line)
    for var_name, decl in missing:
        if f"val {var_name} = _{var_name}.asStateFlow()" in line:
            # check if the private val is not already in the lines
            new_lines.insert(-1, "    " + decl)

with open("app/src/main/java/com/example/ui/GuardianViewModel.kt", "w") as f:
    f.write("\n".join(new_lines))
