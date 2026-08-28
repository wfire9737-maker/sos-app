import re
with open("app/src/main/java/com/example/ui/screens/DeviceMonitoringScreen.kt", "r") as f:
    content = f.read()

# Replace val isConnected = isEsp32Connected || device.status == "CONNECTED"
# with val isConnected = isEsp32Connected
content = re.sub(
    r'val isConnected = isEsp32Connected \|\| device\.status == "CONNECTED"',
    'val isConnected = isEsp32Connected',
    content
)

with open("app/src/main/java/com/example/ui/screens/DeviceMonitoringScreen.kt", "w") as f:
    f.write(content)
