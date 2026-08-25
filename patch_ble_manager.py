import re

with open('app/src/main/java/com/example/ble/BleManager.kt', 'r') as f:
    content = f.read()

# Replace scanning device name
content = content.replace('device.name?.contains("PhysicalSOS-ESP32", true) == true || device.name?.contains("ESP32-SOS", true) == true', 'device.name?.contains("Physical-SOS-ESP32", true) == true')

# Replace SOS characteristic with Status characteristic
content = content.replace('var sosCharacteristic', 'var statusCharacteristic')
content = content.replace('BleProtocol.SOS_CHARACTERISTIC_UUID', 'BleProtocol.STATUS_CHARACTERISTIC_UUID')
content = content.replace('sosCharacteristic = service.getCharacteristic', 'statusCharacteristic = service.getCharacteristic')
content = content.replace('sosCharacteristic?.let {', 'statusCharacteristic?.let {')
content = content.replace('sosCharacteristic = null', 'statusCharacteristic = null')

with open('app/src/main/java/com/example/ble/BleManager.kt', 'w') as f:
    f.write(content)
