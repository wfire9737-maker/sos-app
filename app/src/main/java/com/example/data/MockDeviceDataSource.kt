package com.example.data

import kotlinx.coroutines.delay

class MockDeviceDataSource : DeviceDataSource {
    override suspend fun connect(deviceId: String): Boolean {
        delay(1000)
        return true
    }

    override suspend fun disconnect() {
        delay(500)
    }

    override suspend fun sendCommand(command: String): Boolean {
        delay(500)
        return true
    }

    override suspend fun getBatteryLevel(): Int {
        return 85
    }

    override suspend fun triggerSOS() {
        // Mock SOS trigger
    }
}
