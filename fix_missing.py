import re

with open("app/src/main/java/com/example/ui/GuardianViewModel.kt", "r") as f:
    lines = f.read().split('\n')

missing = [
    ('themeMode', 'private val _themeMode = MutableStateFlow("SYSTEM")'),
    ('language', 'private val _language = MutableStateFlow("en")'),
    ('criticalAlarmsEnabled', 'private val _criticalAlarmsEnabled = MutableStateFlow(true)'),
]

new_lines = []
for line in lines:
    new_lines.append(line)
    for var_name, decl in missing:
        if f"val {var_name} = _{var_name}.asStateFlow()" in line:
            new_lines.insert(-1, "    " + decl)

with open("app/src/main/java/com/example/ui/GuardianViewModel.kt", "w") as f:
    f.write("\n".join(new_lines))
