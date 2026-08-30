import re

with open("app/src/main/java/com/example/ble/nearby/NearbyBleProtocol.kt", "w") as f:
    f.write("""package com.example.ble.nearby

import java.util.UUID

object NearbyBleProtocol {
    // Unique UUID for Android-to-Android Nearby Emergency Presence
    // MUST NOT overlap with Physical-SOS-ESP32 UUIDs
    val NEARBY_SERVICE_UUID: UUID = UUID.fromString("9bf9b53b-0e86-444a-935a-273a0eec26f0")
    val CONNECTION_REQUEST_CHAR_UUID: UUID = UUID.fromString("9bf9b53c-0e86-444a-935a-273a0eec26f0")
    val CONNECTION_STATUS_CHAR_UUID: UUID = UUID.fromString("9bf9b53d-0e86-444a-935a-273a0eec26f0")
}
""")

with open("app/src/main/java/com/example/ble/nearby/NearbyDevice.kt", "w") as f:
    f.write("""package com.example.ble.nearby

data class NearbyDevice(
    val macAddress: String,
    val lastSeen: Long,
    val rssi: Int,
    val connectionState: NearbyConnectionState = NearbyConnectionState.DISCONNECTED
)

enum class NearbyConnectionState {
    DISCONNECTED,
    REQUESTING,
    CONNECTED
}
""")

with open("app/src/main/java/com/example/ble/nearby/NearbyPresenceAdvertiser.kt", "r") as f:
    content = f.read()

content = content.replace(".setConnectable(false)", ".setConnectable(true)")

with open("app/src/main/java/com/example/ble/nearby/NearbyPresenceAdvertiser.kt", "w") as f:
    f.write(content)
