with open('app/src/main/java/com/example/ui/screens/SettingsScreen.kt', 'r') as f:
    lines = f.readlines()
for i, line in enumerate(lines):
    if 'title = "Emergency Phrase",' in line:
        # replace the NEXT line
        lines[i+1] = '                            subtitle = "\\"" + voiceSosPhrase + "\\"",\n'

with open('app/src/main/java/com/example/ui/screens/SettingsScreen.kt', 'w') as f:
    f.writelines(lines)
