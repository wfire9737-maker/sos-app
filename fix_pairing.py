import re

with open("app/src/main/java/com/example/ui/screens/DevicePairingScreen.kt", "r") as f:
    content = f.read()

# Fix private vals
content = content.replace("private val StitchBg =", "val StitchBg2 =")
content = content.replace("private val StitchCard =", "val StitchCard2 =")
content = content.replace("private val StitchRed =", "val StitchRed2 =")
content = content.replace("private val StitchGreen =", "val StitchGreen2 =")
content = content.replace("private val StitchPurple =", "val StitchPurple2 =")
content = content.replace("private val StitchTextMuted =", "val StitchTextMuted2 =")
content = content.replace("private val StitchDarkGray =", "val StitchDarkGray2 =")

content = content.replace("StitchBg", "StitchBg2")
content = content.replace("StitchCard", "StitchCard2")
content = content.replace("StitchRed", "StitchRed2")
content = content.replace("StitchGreen", "StitchGreen2")
content = content.replace("StitchPurple", "StitchPurple2")
content = content.replace("StitchTextMuted", "StitchTextMuted2")
content = content.replace("StitchDarkGray", "StitchDarkGray2")

# Fix device instantiation
content = content.replace(
    'Device("Physical-SOS-ESP32", "00:11:22", "strong", 100)',
    'Device(deviceName = "Physical-SOS-ESP32", macAddress = "00:11:22")'
)

# Fix device property
content = content.replace('device.name', 'device.deviceName')

# Fix connection
content = content.replace('viewModel.connectToEsp32(device.macAddress)', 'viewModel.connectDevice(device.macAddress)')

with open("app/src/main/java/com/example/ui/screens/DevicePairingScreen.kt", "w") as f:
    f.write(content)
