import re

with open('app/src/main/java/com/example/ble/BleManager.kt', 'r') as f:
    content = f.read()

# Remove COMMAND_CHARACTERISTIC_UUID reference
content = re.sub(r'commandCharacteristic = service.getCharacteristic\(BleProtocol\.COMMAND_CHARACTERISTIC_UUID\)', '', content)
content = re.sub(r'private var commandCharacteristic: BluetoothGattCharacteristic\? = null', '', content)
content = re.sub(r'commandCharacteristic = null', '', content)
content = re.sub(r'commandCharacteristic\?\.let \{.*?\}', '', content, flags=re.DOTALL)

with open('app/src/main/java/com/example/ble/BleManager.kt', 'w') as f:
    f.write(content)
