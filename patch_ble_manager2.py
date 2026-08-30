import re

with open("app/src/main/java/com/example/ble/nearby/NearbyBleManager.kt", "r") as f:
    content = f.read()

target = """    private fun updateDeviceConnectionState(macAddress: String, state: NearbyConnectionState) {
        val currentDevices = scanner.nearbyDevices.value
        val device = currentDevices[macAddress]
        if (device != null) {
            val updatedDevice = device.copy(connectionState = state)
            // We need a way to push updates to the scanner's state flow, but scanner owns it.
            // Let's create a proxy stateflow in NearbyBleManager or add update method to scanner.
        }
    }"""
replacement = """    private fun updateDeviceConnectionState(macAddress: String, state: NearbyConnectionState) {
        scanner.updateDeviceConnectionState(macAddress, state)
    }
    
    fun requestConnection(macAddress: String) {
        gattClient.connectToDevice(macAddress)
    }
    
    fun disconnect(macAddress: String) {
        gattClient.disconnect()
        updateDeviceConnectionState(macAddress, NearbyConnectionState.DISCONNECTED)
    }
    
    fun acceptIncomingConnection(macAddress: String) {
        gattServer.acceptConnection(macAddress)
    }
    
    fun declineIncomingConnection(macAddress: String) {
        gattServer.declineConnection(macAddress)
    }"""
content = content.replace(target, replacement)

target_start = """    private fun startPresenceSession() {
        if (isSessionActive || currentIntervalMs <= 0) return
        isSessionActive = true
        // Trigger the first advertisement immediately
        handler.post(advertiseRunnable)
    }"""
replacement_start = """    private fun startPresenceSession() {
        if (isSessionActive || currentIntervalMs <= 0) return
        isSessionActive = true
        gattServer.startServer()
        // Trigger the first advertisement immediately
        handler.post(advertiseRunnable)
    }"""
content = content.replace(target_start, replacement_start)

target_stop = """    private fun stopPresenceSession() {
        isSessionActive = false
        handler.removeCallbacks(advertiseRunnable)
        advertiser.stopAdvertising()
    }"""
replacement_stop = """    private fun stopPresenceSession() {
        isSessionActive = false
        handler.removeCallbacks(advertiseRunnable)
        advertiser.stopAdvertising()
        gattServer.stopServer()
    }"""
content = content.replace(target_stop, replacement_stop)

with open("app/src/main/java/com/example/ble/nearby/NearbyBleManager.kt", "w") as f:
    f.write(content)
