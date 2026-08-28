with open("app/src/main/java/com/example/service/EmergencyService.kt", "r") as f:
    lines = f.readlines()

new_lines = []
for i, line in enumerate(lines):
    new_lines.append(line)
    if 'Log.w("EmergencyService", "CALL_PERMISSION_DENIED: Cannot place call.")' in line:
        # We need to add two more closing braces.
        pass

# It's easier to just do text replacement.
with open("app/src/main/java/com/example/service/EmergencyService.kt", "r") as f:
    content = f.read()

import re
content = re.sub(
    r'Log\.w\("EmergencyService", "CALL_PERMISSION_DENIED: Cannot place call\."\)\s*\}\s*\}\s*\}',
    'Log.w("EmergencyService", "CALL_PERMISSION_DENIED: Cannot place call.")\n                        }\n                    }\n                }\n            }',
    content
)

with open("app/src/main/java/com/example/service/EmergencyService.kt", "w") as f:
    f.write(content)
