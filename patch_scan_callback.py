import re

with open("app/src/main/java/com/example/ble/BleManager.kt", "r") as f:
    content = f.read()

old_scan = """        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            result?.device?.let { device ->
                try {
                    val name = device.name ?: result.scanRecord?.deviceName
                    if (name != null && name.contains(BleProtocol.DEVICE_NAME, ignoreCase = true)) {
                        Log.d("BleManager", "BLE: device discovered")
                        stopScan()
                        connectToDevice(device)
                    }
                } catch (e: SecurityException) {
                    Log.e("BleManager", "SecurityException in scanCallback", e)
                }
            }
        }"""

new_scan = """        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            result?.device?.let { device ->
                try {
                    val name = result.scanRecord?.deviceName ?: try { device.name } catch (e: SecurityException) { null }
                    if (name != null && name.contains(BleProtocol.DEVICE_NAME, ignoreCase = true)) {
                        Log.d("BleManager", "BLE: device discovered")
                        stopScan()
                        connectToDevice(device)
                    }
                } catch (e: Exception) {
                    Log.e("BleManager", "Error in scanCallback", e)
                }
            }
        }"""

content = content.replace(old_scan, new_scan)

with open("app/src/main/java/com/example/ble/BleManager.kt", "w") as f:
    f.write(content)

