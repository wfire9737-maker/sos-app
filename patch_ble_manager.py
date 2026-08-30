import re

with open("app/src/main/java/com/example/ble/nearby/NearbyBleManager.kt", "r") as f:
    content = f.read()

target = """@Singleton
class NearbyBleManager @Inject constructor(
    private val advertiser: NearbyPresenceAdvertiser,
    private val scanner: NearbyDeviceScanner
) {"""

replacement = """@Singleton
class NearbyBleManager @Inject constructor(
    private val advertiser: NearbyPresenceAdvertiser,
    private val scanner: NearbyDeviceScanner,
    private val gattServer: NearbyGattServer,
    private val gattClient: NearbyGattClient
) {"""
content = content.replace(target, replacement)

target2 = """    val nearbyDevices: StateFlow<Map<String, NearbyDevice>> = scanner.nearbyDevices
    
    private val handler = Handler(Looper.getMainLooper())"""
replacement2 = """    val nearbyDevices: StateFlow<Map<String, NearbyDevice>> = scanner.nearbyDevices
    
    private val handler = Handler(Looper.getMainLooper())
    
    init {
        gattServer.onRemoteDeviceDisconnected = { macAddress ->
            updateDeviceConnectionState(macAddress, NearbyConnectionState.DISCONNECTED)
        }
        gattClient.onConnectionStateChanged = { macAddress, newState ->
            updateDeviceConnectionState(macAddress, newState)
        }
    }
    
    private fun updateDeviceConnectionState(macAddress: String, state: NearbyConnectionState) {
        val currentDevices = scanner.nearbyDevices.value
        val device = currentDevices[macAddress]
        if (device != null) {
            val updatedDevice = device.copy(connectionState = state)
            // We need a way to push updates to the scanner's state flow, but scanner owns it.
            // Let's create a proxy stateflow in NearbyBleManager or add update method to scanner.
        }
    }"""
content = content.replace(target2, replacement2)

with open("app/src/main/java/com/example/ble/nearby/NearbyBleManager.kt", "w") as f:
    f.write(content)
