import re

file_path = "app/src/main/AndroidManifest.xml"

with open(file_path, "r") as f:
    content = f.read()

# Add usesCleartextTraffic to application tag
if "android:usesCleartextTraffic" not in content:
    content = content.replace("<application", "<application\n        android:usesCleartextTraffic=\"true\"")

with open(file_path, "w") as f:
    f.write(content)

print("Patch applied to AndroidManifest.xml")
