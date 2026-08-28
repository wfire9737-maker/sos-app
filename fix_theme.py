import re

with open("app/src/main/java/com/example/ui/GuardianViewModel.kt", "r") as f:
    content = f.read()

# Add logic to load theme in init
init_pattern = r'(init \{)'
init_replacement = r'''\1
        val prefs = getApplication<android.app.Application>().getSharedPreferences("smart_sos_settings", android.content.Context.MODE_PRIVATE)
        _themeMode.value = prefs.getString("theme_mode", "SYSTEM") ?: "SYSTEM"
'''
content = re.sub(init_pattern, init_replacement, content, count=1)

# Implement setThemeMode
set_theme_pattern = r'fun setThemeMode\(mode: String\) \{ \}'
set_theme_replacement = r'''fun setThemeMode(mode: String) {
        _themeMode.value = mode
        val prefs = getApplication<android.app.Application>().getSharedPreferences("smart_sos_settings", android.content.Context.MODE_PRIVATE)
        prefs.edit().putString("theme_mode", mode).apply()
    }'''
content = re.sub(set_theme_pattern, set_theme_replacement, content)

with open("app/src/main/java/com/example/ui/GuardianViewModel.kt", "w") as f:
    f.write(content)
