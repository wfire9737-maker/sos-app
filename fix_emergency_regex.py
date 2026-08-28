import re

with open("app/src/main/java/com/example/ui/screens/EmergencyScreen.kt", "r") as f:
    content = f.read()

content = re.sub(
    r'Text\("SMS Dispatched", color = Color.White, fontSize = 14\.sp\)',
    'Text(if (emergency?.contactsNotified?.isNotEmpty() == true) "SMS Dispatched" else "Dispatching SMS...", color = Color.White, fontSize = 14.sp)',
    content
)

content = re.sub(
    r'Text\("Audio Recording\.\.\.", color = Color.White, fontSize = 14\.sp\)',
    'Text("Location Tracking Active", color = Color.White, fontSize = 14.sp)',
    content
)

content = re.sub(
    r'Text\("Emergency Services Pending", color = StitchTextMuted, fontSize = 14\.sp\)',
    'Text(emergency?.responderStatus ?: "Emergency Services Pending", color = StitchTextMuted, fontSize = 14.sp)',
    content
)

with open("app/src/main/java/com/example/ui/screens/EmergencyScreen.kt", "w") as f:
    f.write(content)
