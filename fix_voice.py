import os
filepath = "app/src/main/java/com/example/ui/screens/VoiceSosScreen.kt"
with open(filepath, "r") as f:
    content = f.read()
content = content.replace("key = { it.timestamp }", "key = { it.timestampMs }")
with open(filepath, "w") as f:
    f.write(content)
