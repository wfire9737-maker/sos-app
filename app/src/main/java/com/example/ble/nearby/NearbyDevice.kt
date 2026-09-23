package com.example.ble.nearby

data class NearbyDevice(
    val macAddress: String,
    val deviceName: String = NearbyBleProtocol.DEFAULT_DEVICE_NAME,
    val lastSeen: Long,
    val rssi: Int,
    val connectionState: NearbyConnectionState = NearbyConnectionState.DISCONNECTED
)

enum class NearbyConnectionState {
    DISCONNECTED,
    REQUESTING,
    CONNECTED
}
