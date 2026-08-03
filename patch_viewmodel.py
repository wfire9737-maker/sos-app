import re

with open("app/src/main/java/com/example/ui/GuardianViewModel.kt", "r") as f:
    content = f.read()

new_methods = """
    fun disconnectSimulatedDevice(deviceId: String) {
        viewModelScope.launch {
            deviceService.handleDeviceDisconnect(deviceId)
        }
    }
    
    fun connectSimulatedDevice(deviceId: String) {
        viewModelScope.launch {
            val device = databaseService.devices.value.find { it.deviceId == deviceId }
            if (device != null) {
                databaseService.updateDevice(
                    device.copy(
                        status = "CONNECTED",
                        connectionStatus = "ONLINE",
                        lastSync = System.currentTimeMillis()
                    )
                )
                deviceService.addCommLog("✅ Simulated DEVICE_CONNECTED message processed.")
            }
        }
    }
"""

content = content.replace("    fun triggerManualHeartbeatCheck(deviceId: String) {\n        deviceService.triggerManualHeartbeatCheck(deviceId)\n    }", "    fun triggerManualHeartbeatCheck(deviceId: String) {\n        deviceService.triggerManualHeartbeatCheck(deviceId)\n    }\n" + new_methods)

with open("app/src/main/java/com/example/ui/GuardianViewModel.kt", "w") as f:
    f.write(content)
