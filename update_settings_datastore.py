with open("app/src/main/java/com/example/data/SettingsDataStore.kt", "r") as f:
    content = f.read()

import re

# Add key
content = re.sub(
    r'val SOS_VIBRATION_ENABLED_KEY = booleanPreferencesKey\("sos_vibration_enabled"\)',
    r'val SOS_VIBRATION_ENABLED_KEY = booleanPreferencesKey("sos_vibration_enabled")\n        val CRITICAL_ALARMS_ENABLED_KEY = booleanPreferencesKey("critical_alarms_enabled")',
    content
)

# Add flow
content = re.sub(
    r'val sosVibrationEnabledFlow: Flow<Boolean> = context.dataStore.data\s*\.map { preferences ->\s*preferences\[SOS_VIBRATION_ENABLED_KEY\] \?: true\s*}',
    r'val sosVibrationEnabledFlow: Flow<Boolean> = context.dataStore.data\n        .map { preferences ->\n            preferences[SOS_VIBRATION_ENABLED_KEY] ?: true\n        }\n\n    val criticalAlarmsEnabledFlow: Flow<Boolean> = context.dataStore.data\n        .map { preferences ->\n            preferences[CRITICAL_ALARMS_ENABLED_KEY] ?: true\n        }',
    content
)

# Add setter
content = content + """
    suspend fun setCriticalAlarmsEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[CRITICAL_ALARMS_ENABLED_KEY] = enabled
        }
    }
"""

with open("app/src/main/java/com/example/data/SettingsDataStore.kt", "w") as f:
    f.write(content)
