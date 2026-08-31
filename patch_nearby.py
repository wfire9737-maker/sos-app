import os

gatt_server_path = "app/src/main/java/com/example/ble/nearby/NearbyGattServer.kt"
ble_manager_path = "app/src/main/java/com/example/ble/nearby/NearbyBleManager.kt"

# Patch NearbyGattServer
with open(gatt_server_path, "r") as f:
    gatt = f.read()

target1 = """    // Track connection internally
    private var connectedDevice: BluetoothDevice? = null
    var onRemoteDeviceDisconnected: ((String) -> Unit)? = null

    private val gattServerCallback = object : BluetoothGattServerCallback() {"""
replace1 = """    // Track connection internally
    private var connectedDevice: BluetoothDevice? = null
    var onRemoteDeviceDisconnected: ((String) -> Unit)? = null

    var activeConnections = 0
        private set

    fun hasActiveConnections(): Boolean = activeConnections > 0

    var onActiveConnectionsChanged: ((Int) -> Unit)? = null

    private val gattServerCallback = object : BluetoothGattServerCallback() {"""

target2 = """            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.d("NearbyGattServer", "Device connected: ${device.address}")
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.d("NearbyGattServer", "Device disconnected: ${device.address}")
                if (connectedDevice?.address == device.address) {
                    connectedDevice = null
                    onRemoteDeviceDisconnected?.invoke(device.address)
                }
            }"""
replace2 = """            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.d("NearbyGattServer", "Device connected: ${device.address}")
                activeConnections++
                onActiveConnectionsChanged?.invoke(activeConnections)
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.d("NearbyGattServer", "Device disconnected: ${device.address}")
                if (activeConnections > 0) activeConnections--
                onActiveConnectionsChanged?.invoke(activeConnections)
                if (connectedDevice?.address == device.address) {
                    connectedDevice = null
                    onRemoteDeviceDisconnected?.invoke(device.address)
                }
            }"""

target3 = """            } else {
                try {
                    if (responseNeeded) {
                        gattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_FAILURE, offset, null)
                    }
                } catch (e: SecurityException) {}
            }
        }
    }"""
replace3 = """            } else {
                try {
                    if (responseNeeded) {
                        gattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_FAILURE, offset, null)
                    }
                } catch (e: SecurityException) {}
            }
        }

        override fun onDescriptorWriteRequest(
            device: BluetoothDevice,
            requestId: Int,
            descriptor: BluetoothGattDescriptor,
            preparedWrite: Boolean,
            responseNeeded: Boolean,
            offset: Int,
            value: ByteArray?
        ) {
            super.onDescriptorWriteRequest(device, requestId, descriptor, preparedWrite, responseNeeded, offset, value)
            if (responseNeeded) {
                try {
                    gattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, value)
                } catch (e: SecurityException) {}
            }
        }
    }"""

gatt = gatt.replace(target1, replace1)
gatt = gatt.replace(target2, replace2)
gatt = gatt.replace(target3, replace3)
with open(gatt_server_path, "w") as f:
    f.write(gatt)


# Patch NearbyBleManager
with open(ble_manager_path, "r") as f:
    mgr = f.read()

mtarg1 = """    init {
        gattServer.onRemoteDeviceDisconnected = { macAddress ->
            updateDeviceConnectionState(macAddress, NearbyConnectionState.DISCONNECTED)
        }
        gattClient.onConnectionStateChanged = { macAddress, newState ->"""
mrepl1 = """    init {
        gattServer.onRemoteDeviceDisconnected = { macAddress ->
            updateDeviceConnectionState(macAddress, NearbyConnectionState.DISCONNECTED)
        }
        gattServer.onActiveConnectionsChanged = { count ->
            if (count == 0 && isSessionActive && !isBurstActive) {
                advertiser.stopAdvertising()
            }
        }
        gattClient.onConnectionStateChanged = { macAddress, newState ->"""

mtarg2 = """    fun acceptIncomingConnection(macAddress: String) {
        gattServer.acceptConnection(macAddress)
    }"""
mrepl2 = """    fun acceptIncomingConnection(macAddress: String) {
        gattServer.acceptConnection(macAddress)
        if (isSessionActive && !isBurstActive) {
            advertiser.stopAdvertising()
        }
    }"""

mtarg3 = """    private var currentIntervalMs: Long = 0L
    private var isSessionActive = false
    private val advertiseRunnable = object : Runnable {
        override fun run() {
            if (!isSessionActive || currentIntervalMs <= 0) return
            
            // Expose presence for a short burst (e.g., 2 seconds)
            advertiser.startAdvertising()
            
            handler.postDelayed({
                if (isSessionActive) {
                    advertiser.stopAdvertising()
                }
            }, 2000L) // 2-second burst
            
            // Schedule the next session
            handler.postDelayed(this, currentIntervalMs)
        }
    }"""
mrepl3 = """    private var currentIntervalMs: Long = 0L
    private var isSessionActive = false
    private var isBurstActive = false
    
    private val advertiseRunnable = object : Runnable {
        override fun run() {
            if (!isSessionActive || currentIntervalMs <= 0) return
            
            isBurstActive = true
            // Expose presence for a short burst (e.g., 2 seconds)
            advertiser.startAdvertising()
            
            handler.postDelayed({
                isBurstActive = false
                if (isSessionActive) {
                    if (!gattServer.hasActiveConnections()) {
                        advertiser.stopAdvertising()
                    }
                }
            }, 2000L) // 2-second burst
            
            // Schedule the next session
            handler.postDelayed(this, currentIntervalMs)
        }
    }"""

mgr = mgr.replace(mtarg1, mrepl1)
mgr = mgr.replace(mtarg2, mrepl2)
mgr = mgr.replace(mtarg3, mrepl3)
with open(ble_manager_path, "w") as f:
    f.write(mgr)

