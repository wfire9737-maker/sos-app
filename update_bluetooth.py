import os

filepath = "app/src/main/java/com/example/service/DeviceProvider.kt"
with open(filepath, "r") as f:
    content = f.read()

import_target = "import android.util.Log"
import_replacement = """import android.util.Log
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothAdapter"""
content = content.replace(import_target, import_replacement, 1)

bt_target = """    fun getLocalBluetoothStatus(): String {
        // Bluetooth is typically CONNECTED if the virtual or physical band is paired
        return "CONNECTED"
    }"""

bt_replacement = """    fun getLocalBluetoothStatus(): String {
        return try {
            val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
            val adapter = bluetoothManager?.adapter
            if (adapter?.isEnabled == true) "CONNECTED" else "DISCONNECTED"
        } catch (e: Exception) {
            "DISCONNECTED"
        }
    }"""

content = content.replace(bt_target, bt_replacement, 1)

with open(filepath, "w") as f:
    f.write(content)
print("Updated Bluetooth logic")
