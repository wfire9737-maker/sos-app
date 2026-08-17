with open('app/src/main/java/com/example/ui/screens/SettingsScreen.kt', 'r') as f:
    lines = f.readlines()
for i, line in enumerate(lines):
    if 'subtitle = ""' in line:
        # replace the malformed line
        # it was: subtitle = ""$voiceSosPhrase"",
        lines[i] = '                            subtitle = "\\"" + voiceSosPhrase + "\\"",\n'

with open('app/src/main/java/com/example/ui/screens/SettingsScreen.kt', 'w') as f:
    f.writelines(lines)
