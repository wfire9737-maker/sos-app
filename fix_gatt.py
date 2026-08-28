import re

with open("app/src/main/java/com/example/ble/BleManager.kt", "r") as f:
    content = f.read()

# 1. Modify gattCallback to check gatt equality
cb_start = content.find("private val gattCallback = object : BluetoothGattCallback() {")

# find onConnectionStateChange and insert check
onConnState_start = content.find("override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {", cb_start)

replacement_onConnState = """override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            if (this@BleManager.gatt != gatt) {
                try { gatt.close() } catch (e: Exception) {}
                return
            }
"""

content = content[:onConnState_start] + replacement_onConnState + content[onConnState_start + len("override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {"):]

# Replace other overrides similarly
overrides = [
    ("override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {", "override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {\n            if (this@BleManager.gatt != gatt) return\n"),
    ("override fun onCharacteristicRead(", "override fun onCharacteristicRead(\n            gatt: BluetoothGatt,\n            characteristic: BluetoothGattCharacteristic,\n            status: Int\n        ) {\n            if (this@BleManager.gatt != gatt) return\n"),
    ("override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {", "override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {\n            if (this@BleManager.gatt != gatt) return\n"),
    ("override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, value: ByteArray) {", "override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, value: ByteArray) {\n            if (this@BleManager.gatt != gatt) return\n"),
    ("override fun onReadRemoteRssi(gatt: BluetoothGatt, rssi: Int, status: Int) {", "override fun onReadRemoteRssi(gatt: BluetoothGatt, rssi: Int, status: Int) {\n            if (this@BleManager.gatt != gatt) return\n")
]

for old, new in overrides:
    # Need to be careful with multiline
    if old.startswith("override fun onCharacteristicRead("):
        # Already manually matching it below with regex
        content = re.sub(
            r'override fun onCharacteristicRead\(\s*gatt: BluetoothGatt,\s*characteristic: BluetoothGattCharacteristic,\s*status: Int\s*\)\s*\{',
            new,
            content
        )
    else:
        content = content.replace(old, new)


# Update cleanGatt
clean_gatt_old = """private fun cleanGatt() {
        disconnectGattInternal()
    }"""
clean_gatt_new = """private fun cleanGatt() {
        disconnectGattInternal()
    }"""
# wait, actually let's update disconnectGattInternal
disconnectGattInternal_old = """private fun disconnectGattInternal() {
        try {
            gatt?.disconnect()
            gatt?.close()
        } catch (e: SecurityException) {
            Log.e("BleManager", "SecurityException closing GATT", e)
        } finally {
            gatt = null
            _rssi.value = null
        }
    }"""
disconnectGattInternal_new = """private fun disconnectGattInternal() {
        val oldGatt = gatt
        gatt = null
        try {
            oldGatt?.disconnect()
            oldGatt?.close()
        } catch (e: SecurityException) {
            Log.e("BleManager", "SecurityException closing GATT", e)
        } catch (e: Exception) {
            Log.e("BleManager", "Exception closing GATT", e)
        } finally {
            _rssi.value = null
        }
    }"""

content = content.replace(disconnectGattInternal_old, disconnectGattInternal_new)

# connectToDevice should prevent multiple connections
connectToDevice_old = """private fun connectToDevice(device: BluetoothDevice) {
        try {
            _connectionState.value = BleState.CONNECTING
            Log.d("BleManager", "BLE: connecting")
            cleanGatt()
            gatt = device.connectGatt(context, false, gattCallback)
        } catch (e: SecurityException) {"""
connectToDevice_new = """private fun connectToDevice(device: BluetoothDevice) {
        try {
            if (_connectionState.value == BleState.CONNECTING && gatt != null) {
                Log.d("BleManager", "BLE: Already connecting, ignoring duplicate request")
                return
            }
            _connectionState.value = BleState.CONNECTING
            Log.d("BleManager", "BLE: connecting")
            cleanGatt()
            gatt = device.connectGatt(context, false, gattCallback)
        } catch (e: SecurityException) {"""

content = content.replace(connectToDevice_old, connectToDevice_new)


with open("app/src/main/java/com/example/ble/BleManager.kt", "w") as f:
    f.write(content)

