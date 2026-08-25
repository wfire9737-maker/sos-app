import re

with open('app/src/main/java/com/example/ble/BleManager.kt', 'r') as f:
    content = f.read()

target = """                        statusCharacteristic?.let {
                            gatt.setCharacteristicNotification(it, true)
                            val descriptor = it.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"))
                            if (descriptor != null) {
                                descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                                gatt.writeDescriptor(descriptor)
                            }
                        }"""

replacement = """                        statusCharacteristic?.let {
                            gatt.setCharacteristicNotification(it, true)
                            val descriptor = it.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"))
                            if (descriptor != null) {
                                descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                                gatt.writeDescriptor(descriptor)
                            }
                            handler.postDelayed({
                                try {
                                    gatt.readCharacteristic(it)
                                } catch (e: SecurityException) {
                                    // ignore
                                }
                            }, 500)
                        }"""

content = content.replace(target, replacement)

with open('app/src/main/java/com/example/ble/BleManager.kt', 'w') as f:
    f.write(content)
