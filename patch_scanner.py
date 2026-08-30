import re

with open("app/src/main/java/com/example/ble/nearby/NearbyDeviceScanner.kt", "r") as f:
    content = f.read()

target = """    fun stopScanning() {"""
replacement = """    fun updateDeviceConnectionState(macAddress: String, state: NearbyConnectionState) {
        val updatedMap = _nearbyDevices.value.toMutableMap()
        val device = updatedMap[macAddress]
        if (device != null) {
            updatedMap[macAddress] = device.copy(connectionState = state)
            _nearbyDevices.value = updatedMap
        }
    }
    
    fun stopScanning() {"""
content = content.replace(target, replacement)

# Add connectionState handling to onScanResult so we don't overwrite connection state
target2 = """                    val newDevice = NearbyDevice(macAddress = address, lastSeen = timestamp, rssi = rssi)
                    
                    val updatedMap = _nearbyDevices.value.toMutableMap()
                    updatedMap[address] = newDevice
                    _nearbyDevices.value = updatedMap"""
replacement2 = """                    val updatedMap = _nearbyDevices.value.toMutableMap()
                    val existingDevice = updatedMap[address]
                    val currentState = existingDevice?.connectionState ?: NearbyConnectionState.DISCONNECTED
                    
                    val newDevice = NearbyDevice(macAddress = address, lastSeen = timestamp, rssi = rssi, connectionState = currentState)
                    
                    updatedMap[address] = newDevice
                    _nearbyDevices.value = updatedMap"""
content = content.replace(target2, replacement2)

with open("app/src/main/java/com/example/ble/nearby/NearbyDeviceScanner.kt", "w") as f:
    f.write(content)
