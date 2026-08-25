import re

with open('app/src/main/java/com/example/ui/GuardianViewModel.kt', 'r') as f:
    content = f.read()

# Replace hardcoded Initializations with SharedPreferences lookups
content = content.replace(
    'private val _themeMode = MutableStateFlow("SYSTEM")',
    'private val _themeMode = MutableStateFlow(try { getApplication<Application>().getSharedPreferences("smart_sos_settings", Context.MODE_PRIVATE).getString("theme_mode", "SYSTEM") ?: "SYSTEM" } catch(e:Exception) { "SYSTEM" })'
)

content = content.replace(
    'private val _language = MutableStateFlow("en")',
    'private val _language = MutableStateFlow(try { getApplication<Application>().getSharedPreferences("smart_sos_settings", Context.MODE_PRIVATE).getString("language", "en") ?: "en" } catch(e:Exception) { "en" })'
)

content = content.replace(
    'private val _criticalAlarmsEnabled = MutableStateFlow(true)',
    'private val _criticalAlarmsEnabled = MutableStateFlow(try { getApplication<Application>().getSharedPreferences("smart_sos_settings", Context.MODE_PRIVATE).getBoolean("critical_alarms_enabled", true) } catch(e:Exception) { true })'
)

with open('app/src/main/java/com/example/ui/GuardianViewModel.kt', 'w') as f:
    f.write(content)
