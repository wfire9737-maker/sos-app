package com.example.data

interface DeviceDataSource {
    suspend fun connect(deviceId: String): Boolean
    suspend fun disconnect()
    suspend fun sendCommand(command: String): Boolean
    suspend fun getBatteryLevel(): Int
    suspend fun triggerSOS()
}
