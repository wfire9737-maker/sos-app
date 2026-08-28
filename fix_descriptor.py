import re

with open("app/src/main/java/com/example/ble/BleManager.kt", "r") as f:
    content = f.read()

old = "override fun onDescriptorWrite("
new = "override fun onDescriptorWrite(\n            gatt: BluetoothGatt,\n            descriptor: BluetoothGattDescriptor,\n            status: Int\n        ) {\n            if (this@BleManager.gatt != gatt) return\n"

content = re.sub(
    r'override fun onDescriptorWrite\(\s*gatt: BluetoothGatt,\s*descriptor: BluetoothGattDescriptor,\s*status: Int\s*\)\s*\{',
    new,
    content
)

with open("app/src/main/java/com/example/ble/BleManager.kt", "w") as f:
    f.write(content)
