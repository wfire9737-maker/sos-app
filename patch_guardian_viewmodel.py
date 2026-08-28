with open("app/src/main/java/com/example/ui/GuardianViewModel.kt", "r") as f:
    content = f.read()

import re

# Update critical alarms flow
content = re.sub(
    r'private val _criticalAlarmsEnabled = MutableStateFlow\(true\)\s*val criticalAlarmsEnabled = _criticalAlarmsEnabled\.asStateFlow\(\)',
    r'val criticalAlarmsEnabled = settingsDataStore.criticalAlarmsEnabledFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)',
    content
)

# Replace empty functions
content = re.sub(
    r'fun setSosSoundEnabled\(enabled: Boolean\) \{ \}',
    r'fun setSosSoundEnabled(enabled: Boolean) { viewModelScope.launch { settingsDataStore.setSosSoundEnabled(enabled) } }',
    content
)

content = re.sub(
    r'fun setSosVibrationEnabled\(enabled: Boolean\) \{ \}',
    r'fun setSosVibrationEnabled(enabled: Boolean) { viewModelScope.launch { settingsDataStore.setSosVibrationEnabled(enabled) } }',
    content
)

content = re.sub(
    r'fun setCriticalAlarmsEnabled\(enabled: Boolean\) \{ \}',
    r'fun setCriticalAlarmsEnabled(enabled: Boolean) { viewModelScope.launch { settingsDataStore.setCriticalAlarmsEnabled(enabled) } }',
    content
)

with open("app/src/main/java/com/example/ui/GuardianViewModel.kt", "w") as f:
    f.write(content)
