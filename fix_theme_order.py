import re

with open("app/src/main/java/com/example/ui/GuardianViewModel.kt", "r") as f:
    content = f.read()

# Remove from init
content = content.replace(
'''        val prefs = getApplication<android.app.Application>().getSharedPreferences("smart_sos_settings", android.content.Context.MODE_PRIVATE)
        _themeMode.value = prefs.getString("theme_mode", "SYSTEM") ?: "SYSTEM"''',
'')

# Update declaration
decl_pattern = r'private val _themeMode = MutableStateFlow\("SYSTEM"\)'
decl_replacement = r'private val _themeMode = MutableStateFlow(getApplication<android.app.Application>().getSharedPreferences("smart_sos_settings", android.content.Context.MODE_PRIVATE).getString("theme_mode", "SYSTEM") ?: "SYSTEM")'

content = re.sub(decl_pattern, decl_replacement, content)

with open("app/src/main/java/com/example/ui/GuardianViewModel.kt", "w") as f:
    f.write(content)
