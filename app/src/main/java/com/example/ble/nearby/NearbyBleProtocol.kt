package com.example.ble.nearby

import java.util.UUID

object NearbyBleProtocol {
    // Unique UUID for Android-to-Android Nearby Emergency Presence
    // MUST NOT overlap with Physical-SOS-ESP32 UUIDs
    val NEARBY_SERVICE_UUID: UUID = UUID.fromString("9bf9b53b-0e86-444a-935a-273a0eec26f0")
    val CONNECTION_REQUEST_CHAR_UUID: UUID = UUID.fromString("9bf9b53c-0e86-444a-935a-273a0eec26f0")
    val CONNECTION_STATUS_CHAR_UUID: UUID = UUID.fromString("9bf9b53d-0e86-444a-935a-273a0eec26f0")
    val NEARBY_PAYLOAD_CHAR_UUID: UUID = UUID.fromString("9bf9b53e-0e86-444a-935a-273a0eec26f0")

    // Maximum safe payload size without advanced MTU chunking
    const val MAX_PAYLOAD_SIZE = 512

    // 16-bit UUID for Nearby Device Name & Stable ID advertising service data
    val NEARBY_NAME_SERVICE_UUID: UUID = UUID.fromString("00009bf9-0000-1000-8000-00805f9b34fb")
    
    const val DEFAULT_DEVICE_NAME = "Smart SOS Phone"
    const val PREFS_KEY_STABLE_DEVICE_ID = "nearby_stable_device_id"
}
