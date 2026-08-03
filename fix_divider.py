import os

filepath = "app/src/main/java/com/example/ui/screens/DevicePairingScreen.kt"
with open(filepath, "r") as f:
    content = f.read()

content = content.replace("HorizontalHorizontalDivider", "HorizontalDivider")

with open(filepath, "w") as f:
    f.write(content)
