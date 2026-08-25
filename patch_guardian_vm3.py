import re

with open('app/src/main/java/com/example/ui/GuardianViewModel.kt', 'r') as f:
    content = f.read()

content = content.replace(
    'fun setThemeMode(mode: String) { _themeMode.value = mode }',
    'fun setThemeMode(mode: String) { _themeMode.value = mode; databaseService.saveUserSetting("theme_mode", mode) }'
)

content = content.replace(
    'fun setLanguage(lang: String) { _language.value = lang }',
    'fun setLanguage(lang: String) { _language.value = lang; databaseService.saveUserSetting("language", lang) }'
)

content = content.replace(
    'fun setCriticalAlarmsEnabled(enabled: Boolean) { _criticalAlarmsEnabled.value = enabled }',
    'fun setCriticalAlarmsEnabled(enabled: Boolean) { _criticalAlarmsEnabled.value = enabled; databaseService.saveUserSetting("critical_alarms_enabled", enabled) }'
)

with open('app/src/main/java/com/example/ui/GuardianViewModel.kt', 'w') as f:
    f.write(content)
