package com.example.ble.nearby

import java.util.UUID

object NearbyBleProtocol {
    // Unique UUID for Android-to-Android Nearby Emergency Presence
    // MUST NOT overlap with Physical-SOS-ESP32 UUIDs
    val NEARBY_SERVICE_UUID: UUID = UUID.fromString("9bf9b53b-0e86-444a-935a-273a0eec26f0")
    val CONNECTION_REQUEST_CHAR_UUID: UUID = UUID.fromString("9bf9b53c-0e86-444a-935a-273a0eec26f0")
    val CONNECTION_STATUS_CHAR_UUID: UUID = UUID.fromString("9bf9b53d-0e86-444a-935a-273a0eec26f0")
}
