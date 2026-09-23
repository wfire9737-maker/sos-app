package com.example.ble.nearby

import android.os.Handler
import android.os.Looper
import android.util.Log
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NearbyBleManager @Inject constructor(
    private val advertiser: NearbyPresenceAdvertiser,
    private val scanner: NearbyDeviceScanner,
    private val gattServer: NearbyGattServer,
    private val gattClient: NearbyGattClient
) {
    companion object {
        private const val TAG = "NearbyBleManager"
        private const val CONNECTION_TIMEOUT_MS = 20_000L // 20-second safety timeout
        private const val PRESENCE_BURST_DURATION_MS = 2_000L // 2-second presence burst
        private const val STALE_DEVICE_THRESHOLD_MS = 60_000L // 60-second stale check
    }

    val nearbyDevices: StateFlow<Map<String, NearbyDevice>> = scanner.nearbyDevices
    
    private val _incomingPayloads = MutableSharedFlow<Pair<String, String>>(extraBufferCapacity = 10)
    val incomingPayloads = _incomingPayloads.asSharedFlow()
    
    private val handler = Handler(Looper.getMainLooper())

    private var currentIntervalMs: Long = 0L
    private var isSessionActive = false
    private var isBurstActive = false
    private var isConnectionPending = false
    private var pendingConnectionMac: String? = null

    private val connectionTimeoutRunnable = Runnable {
        Log.w(TAG, "Connection attempt timed out for $pendingConnectionMac")
        val targetMac = pendingConnectionMac
        isConnectionPending = false
        pendingConnectionMac = null
        
        if (targetMac != null) {
            gattClient.disconnect()
            updateDeviceConnectionState(targetMac, NearbyConnectionState.DISCONNECTED)
        }
        
        // Stop temporary connection advertising if not in burst and no active server connections
        if (isSessionActive && !isBurstActive && !gattServer.hasActiveConnections()) {
            advertiser.stopAdvertising()
        }
    }
    
    init {
        gattServer.onRemoteDeviceDisconnected = { macAddress ->
            updateDeviceConnectionState(macAddress, NearbyConnectionState.DISCONNECTED)
        }
        gattServer.onConnectionRequested = { macAddress, senderName ->
            updateDeviceConnectionState(macAddress, NearbyConnectionState.DISCONNECTED, senderName)
        }
        gattServer.onActiveConnectionsChanged = { count ->
            if (count == 0 && isSessionActive && !isBurstActive && !isConnectionPending) {
                advertiser.stopAdvertising()
            }
        }
        gattServer.onIncomingPayloadReceived = { macAddress, payload ->
            _incomingPayloads.tryEmit(macAddress to payload)
            Log.d(TAG, "Received payload from (Server role): $macAddress")
        }
        gattClient.onConnectionStateChanged = { macAddress, newState ->
            updateDeviceConnectionState(macAddress, newState)
            if (newState == NearbyConnectionState.CONNECTED) {
                Log.d(TAG, "Connection established successfully with $macAddress")
                handler.removeCallbacks(connectionTimeoutRunnable)
                isConnectionPending = false
                pendingConnectionMac = null
                // Stop unnecessary presence advertising while preserving active GATT connection
                if (isSessionActive && !isBurstActive && !gattServer.hasActiveConnections()) {
                    advertiser.stopAdvertising()
                }
            } else if (newState == NearbyConnectionState.DISCONNECTED) {
                if (pendingConnectionMac == macAddress) {
                    Log.d(TAG, "Connection disconnected/failed with $macAddress")
                    handler.removeCallbacks(connectionTimeoutRunnable)
                    isConnectionPending = false
                    pendingConnectionMac = null
                    if (isSessionActive && !isBurstActive && !gattServer.hasActiveConnections()) {
                        advertiser.stopAdvertising()
                    }
                }
            }
        }
        gattClient.onIncomingPayloadReceived = { macAddress, payload ->
            _incomingPayloads.tryEmit(macAddress to payload)
            Log.d(TAG, "Received payload from (Client role): $macAddress")
        }
    }
    
    private fun updateDeviceConnectionState(macAddress: String, state: NearbyConnectionState, deviceName: String? = null) {
        scanner.updateDeviceConnectionState(macAddress, state, deviceName)
    }
    
    fun requestConnection(macAddress: String) {
        val device = nearbyDevices.value[macAddress]
        if (device != null && System.currentTimeMillis() - device.lastSeen > STALE_DEVICE_THRESHOLD_MS) {
            Log.w(TAG, "Device $macAddress is stale (last seen > 60s ago), ignoring connection request.")
            updateDeviceConnectionState(macAddress, NearbyConnectionState.DISCONNECTED)
            return
        }

        pendingConnectionMac = macAddress
        isConnectionPending = true

        // Ensure advertising remains connectable during connection handshake
        if (isSessionActive) {
            advertiser.startAdvertising()
        }

        updateDeviceConnectionState(macAddress, NearbyConnectionState.REQUESTING)

        handler.removeCallbacks(connectionTimeoutRunnable)
        handler.postDelayed(connectionTimeoutRunnable, CONNECTION_TIMEOUT_MS)

        gattClient.connectToDevice(macAddress)
    }
    
    fun disconnect(macAddress: String) {
        handler.removeCallbacks(connectionTimeoutRunnable)
        isConnectionPending = false
        pendingConnectionMac = null
        gattClient.disconnect()
        gattServer.disconnectDevice(macAddress)
        updateDeviceConnectionState(macAddress, NearbyConnectionState.DISCONNECTED)
        if (isSessionActive && !isBurstActive && !gattServer.hasActiveConnections()) {
            advertiser.stopAdvertising()
        }
    }
    
    fun acceptIncomingConnection(macAddress: String) {
        updateDeviceConnectionState(macAddress, NearbyConnectionState.CONNECTED)
        gattServer.acceptConnection(macAddress)
        if (isSessionActive && !isBurstActive && !isConnectionPending && !gattServer.hasActiveConnections()) {
            advertiser.stopAdvertising()
        }
    }
    
    fun declineIncomingConnection(macAddress: String) {
        updateDeviceConnectionState(macAddress, NearbyConnectionState.DISCONNECTED)
        gattServer.declineConnection(macAddress)
    }

    fun sendNearbyPayload(macAddress: String, payload: String): Boolean {
        val device = nearbyDevices.value[macAddress]
        if (device == null || device.connectionState != NearbyConnectionState.CONNECTED) {
            Log.w(TAG, "Cannot send payload: Device $macAddress is not connected.")
            return false
        }
        
        // Try server side first (if we accepted their request)
        var success = gattServer.sendPayloadNotification(macAddress, payload)
        
        if (!success) {
            // Try client side (if we initiated the request)
            success = gattClient.sendPayload(payload)
        }
        
        if (success) {
            Log.d(TAG, "Payload sent successfully to $macAddress")
        } else {
            Log.e(TAG, "Failed to send payload to $macAddress")
        }
        return success
    }

    private val advertiseRunnable = object : Runnable {
        override fun run() {
            if (!isSessionActive || currentIntervalMs <= 0) return
            
            // Expose presence for a short burst (e.g., 2 seconds)
            isBurstActive = true
            advertiser.startAdvertising()
            
            handler.postDelayed({
                isBurstActive = false
                if (isSessionActive) {
                    // Do NOT stop advertising if a connection is being requested or in progress
                    if (!gattServer.hasActiveConnections() && !isConnectionPending) {
                        advertiser.stopAdvertising()
                    }
                }
            }, PRESENCE_BURST_DURATION_MS)

            // Schedule the next session
            handler.postDelayed(this, currentIntervalMs)
        }
    }

    fun updatePresenceSettings(intervalSeconds: Int) {
        val wasActive = isSessionActive
        stopPresenceSession()
        
        if (intervalSeconds > 0) {
            currentIntervalMs = intervalSeconds * 1000L
            startPresenceSession()
        }
    }

    private fun startPresenceSession() {
        if (isSessionActive || currentIntervalMs <= 0) return
        isSessionActive = true
        gattServer.startServer()
        // Trigger the first advertisement immediately
        handler.post(advertiseRunnable)
    }

    private fun stopPresenceSession() {
        isSessionActive = false
        isBurstActive = false
        isConnectionPending = false
        pendingConnectionMac = null
        handler.removeCallbacks(connectionTimeoutRunnable)
        handler.removeCallbacks(advertiseRunnable)
        advertiser.stopAdvertising()
        gattServer.stopServer()
    }

    fun startAdvertisingPresence() {
        advertiser.startAdvertising()
    }

    fun stopAdvertisingPresence() {
        advertiser.stopAdvertising()
    }

    fun startScanningForNearby() {
        scanner.startScanning()
    }

    fun stopScanningForNearby() {
        scanner.stopScanning()
    }
}
