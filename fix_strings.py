import re

with open('app/src/main/java/com/example/ui/screens/BleTestScreen.kt', 'r') as f:
    content = f.read()

content = content.replace('Text("Device:\nPhysical-SOS-ESP32"', 'Text("Device:\\nPhysical-SOS-ESP32"')
content = content.replace('Text("Connection status:\n$displayState"', 'Text("Connection status:\\n$displayState"')
content = content.replace('Text("Service:\n${BleProtocol.SERVICE_UUID}"', 'Text("Service:\\n${BleProtocol.SERVICE_UUID}"')
content = content.replace('Text("Status characteristic:\n${BleProtocol.STATUS_CHARACTERISTIC_UUID}"', 'Text("Status characteristic:\\n${BleProtocol.STATUS_CHARACTERISTIC_UUID}"')
content = content.replace('Text("Battery:\n$displayBattery"', 'Text("Battery:\\n$displayBattery"')
content = content.replace('Text("Status Event (Is SOS?):\n$sosEvent"', 'Text("Status Event (Is SOS?):\\n$sosEvent"')

with open('app/src/main/java/com/example/ui/screens/BleTestScreen.kt', 'w') as f:
    f.write(content)
