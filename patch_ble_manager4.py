import re

with open('app/src/main/java/com/example/ble/BleManager.kt', 'r') as f:
    content = f.read()

target = """        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            if (status == BluetoothGatt.GATT_SUCCESS && characteristic.uuid == BleProtocol.BATTERY_CHARACTERISTIC_UUID) {
                val value = characteristic.value
                if (value != null && value.isNotEmpty()) {
                    _batteryLevel.value = value[0].toInt()
                }
            }
        }"""

replacement = """        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                val value = characteristic.value
                if (characteristic.uuid == BleProtocol.BATTERY_CHARACTERISTIC_UUID) {
                    if (value != null && value.isNotEmpty()) {
                        _batteryLevel.value = value[0].toInt()
                    }
                } else if (characteristic.uuid == BleProtocol.STATUS_CHARACTERISTIC_UUID) {
                    val isSos = value != null && value.isNotEmpty() && value[0].toInt() != 0
                    _sosEvent.value = isSos
                }
            }
        }"""

content = content.replace(target, replacement)

target2 = """        override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
            if (characteristic.uuid == BleProtocol.STATUS_CHARACTERISTIC_UUID) {
                val value = characteristic.value
                val isSos = value != null && value.isNotEmpty() && value[0].toInt() != 0
                _sosEvent.value = isSos
            } else if (characteristic.uuid == BleProtocol.BATTERY_CHARACTERISTIC_UUID) {
                val value = characteristic.value
                if (value != null && value.isNotEmpty()) {
                    _batteryLevel.value = value[0].toInt()
                }
            }
        }"""

# Actually, let's just make sure target2 is exactly what is in the file.
with open('app/src/main/java/com/example/ble/BleManager.kt', 'w') as f:
    f.write(content)
