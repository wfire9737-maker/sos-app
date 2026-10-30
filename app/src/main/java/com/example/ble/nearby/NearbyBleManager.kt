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

    private var activeConnectedPeerMac: String? = null
    val activeConnectedPeer: String?
        get() = activeConnectedPeerMac

    fun getCurrentActivePeer(): String? {
        return activeConnectedPeerMac
            ?: gattClient.connectedDeviceAddress
            ?: gattServer.connectedDeviceAddress
            ?: scanner.nearbyDevices.value.values.firstOrNull { it.connectionState == NearbyConnectionState.CONNECTED }?.let { it.macAddress ?: it.id }
    }

    private val connectionTimeoutRunnable = Runnable {
        Log.w(TAG, "Connection attempt timed out for $pendingConnectionMac")
        val targetMac = pendingConnectionMac
        isConnectionPending = false
        pendingConnectionMac = null
        
        if (targetMac != null) {
            gattClient.disconnect()
            gattClient.closeGatt()
            updateDeviceConnectionState(targetMac, NearbyConnectionState.DISCONNECTED)
            if (activeConnectedPeerMac.equals(targetMac, ignoreCase = true)) {
                activeConnectedPeerMac = null
            }
        }
        scanner.enforceSingleConnectedPeer(activeConnectedPeerMac)
        
        // Stop temporary connection advertising if not in burst and no active connections
        if (isSessionActive && !isBurstActive && !hasActiveConnection()) {
            advertiser.stopAdvertising()
            handler.removeCallbacks(advertiseRunnable)
            handler.post(advertiseRunnable)
        }
    }
    
    init {
        gattServer.onRemoteDeviceDisconnected = { macAddress ->
            Log.d(TAG, "GattServer remote device disconnected: $macAddress")
            if (activeConnectedPeerMac.equals(macAddress, ignoreCase = true)) {
                activeConnectedPeerMac = null
            }
            updateDeviceConnectionState(macAddress, NearbyConnectionState.DISCONNECTED)
            scanner.enforceSingleConnectedPeer(activeConnectedPeerMac)
            if (isSessionActive && !hasActiveConnection() && !isConnectionPending) {
                handler.removeCallbacks(advertiseRunnable)
                handler.post(advertiseRunnable)
            }
        }
        gattServer.onConnectionRequested = { macAddress, senderName ->
            val currentActive = getCurrentActivePeer()
            if (currentActive != null && !currentActive.equals(macAddress, ignoreCase = true) && hasActiveConnection()) {
                Log.w(TAG, "Incoming connection request from $macAddress rejected because peer $currentActive is currently connected.")
                gattServer.declineConnection(macAddress)
                updateDeviceConnectionState(macAddress, NearbyConnectionState.DISCONNECTED, senderName)
            } else {
                updateDeviceConnectionState(macAddress, NearbyConnectionState.REQUESTING, senderName)
            }
        }
        gattServer.onActiveConnectionsChanged = { count ->
            if (count == 0) {
                if (activeConnectedPeerMac != null && !gattClient.isConnected) {
                    activeConnectedPeerMac = null
                    scanner.enforceSingleConnectedPeer(null)
                }
                if (isSessionActive && !isBurstActive && !isConnectionPending && !hasActiveConnection()) {
                    advertiser.stopAdvertising()
                    handler.removeCallbacks(advertiseRunnable)
                    handler.post(advertiseRunnable)
                }
            }
        }
        gattServer.onIncomingPayloadReceived = { macAddress, payload ->
            handler.removeCallbacks(connectionTimeoutRunnable)
            isConnectionPending = false
            pendingConnectionMac = null
            activeConnectedPeerMac = macAddress
            updateDeviceConnectionState(macAddress, NearbyConnectionState.CONNECTED)
            scanner.enforceSingleConnectedPeer(macAddress)
            advertiser.stopAdvertising()
            isBurstActive = false
            _incomingPayloads.tryEmit(macAddress to payload)
            Log.d(TAG, "Received payload from (Server role): $macAddress")
        }
        gattClient.onConnectionStateChanged = { macAddress, newState ->
            if (newState == NearbyConnectionState.CONNECTED) {
                Log.d(TAG, "Connection established successfully with $macAddress")
                handler.removeCallbacks(connectionTimeoutRunnable)
                isConnectionPending = false
                pendingConnectionMac = null
                activeConnectedPeerMac = macAddress
                updateDeviceConnectionState(macAddress, NearbyConnectionState.CONNECTED)
                scanner.enforceSingleConnectedPeer(macAddress)
                // Stop presence advertising while preserving active GATT connection
                advertiser.stopAdvertising()
                isBurstActive = false
            } else if (newState == NearbyConnectionState.DISCONNECTED) {
                Log.d(TAG, "Connection disconnected with $macAddress")
                handler.removeCallbacks(connectionTimeoutRunnable)
                isConnectionPending = false
                pendingConnectionMac = null
                if (activeConnectedPeerMac.equals(macAddress, ignoreCase = true)) {
                    activeConnectedPeerMac = null
                }
                updateDeviceConnectionState(macAddress, NearbyConnectionState.DISCONNECTED)
                scanner.enforceSingleConnectedPeer(activeConnectedPeerMac)
                if (isSessionActive && !hasActiveConnection()) {
                    advertiser.stopAdvertising()
                    handler.removeCallbacks(advertiseRunnable)
                    handler.post(advertiseRunnable)
                }
            } else {
                updateDeviceConnectionState(macAddress, newState)
            }
        }
        gattClient.onIncomingPayloadReceived = { macAddress, payload ->
            handler.removeCallbacks(connectionTimeoutRunnable)
            isConnectionPending = false
            pendingConnectionMac = null
            activeConnectedPeerMac = macAddress
            updateDeviceConnectionState(macAddress, NearbyConnectionState.CONNECTED)
            scanner.enforceSingleConnectedPeer(macAddress)
            advertiser.stopAdvertising()
            isBurstActive = false
            _incomingPayloads.tryEmit(macAddress to payload)
            Log.d(TAG, "Received payload from (Client role): $macAddress")
        }
    }
    
    private fun updateDeviceConnectionState(identifier: String, state: NearbyConnectionState, deviceName: String? = null) {
        scanner.updateDeviceConnectionState(identifier, state, deviceName)
    }

    fun hasActiveConnection(): Boolean {
        return gattServer.hasActiveConnections() || gattClient.isConnected
    }

    fun findDevice(identifier: String): NearbyDevice? {
        return nearbyDevices.value[identifier]
            ?: nearbyDevices.value.values.firstOrNull { it.id == identifier || it.macAddress == identifier }
    }
    
    private fun disconnectInternal(macAddress: String) {
        Log.d(TAG, "Tearing down connection for $macAddress")
        handler.removeCallbacks(connectionTimeoutRunnable)
        if (pendingConnectionMac.equals(macAddress, ignoreCase = true)) {
            isConnectionPending = false
            pendingConnectionMac = null
        }
        gattClient.disconnect()
        gattClient.closeGatt()
        gattServer.disconnectDevice(macAddress)
        updateDeviceConnectionState(macAddress, NearbyConnectionState.DISCONNECTED)
        if (activeConnectedPeerMac.equals(macAddress, ignoreCase = true)) {
            activeConnectedPeerMac = null
        }
        scanner.enforceSingleConnectedPeer(activeConnectedPeerMac)
    }

    fun requestConnection(target: String) {
        val device = findDevice(target)
        val targetMac = device?.macAddress ?: target

        // Prevent duplicate connection if already connected to this peer
        if ((device?.connectionState == NearbyConnectionState.CONNECTED && activeConnectedPeerMac.equals(targetMac, ignoreCase = true)) ||
            (gattClient.isConnected && gattClient.connectedDeviceAddress.equals(targetMac, ignoreCase = true))) {
            Log.d(TAG, "Device $targetMac is already connected. Skipping duplicate connection.")
            return
        }

        // Prevent duplicate connection if already requesting this peer
        if (isConnectionPending && pendingConnectionMac.equals(targetMac, ignoreCase = true)) {
            Log.d(TAG, "Connection already pending for $targetMac. Skipping duplicate connection.")
            return
        }

        // Single Active Peer Rule: If connected to another peer, disconnect it completely first
        val currentActive = getCurrentActivePeer()
        if (currentActive != null && !currentActive.equals(targetMac, ignoreCase = true)) {
            Log.d(TAG, "Single active peer rule: Disconnecting existing peer $currentActive before connecting to $targetMac")
            disconnectInternal(currentActive)
        }

        // If another pending connection exists, clean it up
        if (isConnectionPending && pendingConnectionMac != null) {
            val oldPending = pendingConnectionMac!!
            handler.removeCallbacks(connectionTimeoutRunnable)
            gattClient.disconnect()
            gattClient.closeGatt()
            updateDeviceConnectionState(oldPending, NearbyConnectionState.DISCONNECTED)
            isConnectionPending = false
            pendingConnectionMac = null
        }

        if (device != null && System.currentTimeMillis() - device.lastSeen > STALE_DEVICE_THRESHOLD_MS) {
            Log.w(TAG, "Device $target is stale (last seen > 60s ago), ignoring connection request.")
            updateDeviceConnectionState(targetMac, NearbyConnectionState.DISCONNECTED)
            return
        }

        pendingConnectionMac = targetMac
        isConnectionPending = true

        // Ensure advertising remains connectable during connection handshake
        if (isSessionActive) {
            advertiser.startAdvertising()
        }

        // Mark targetMac as REQUESTING (NOT CONNECTED - only mark CONNECTED once GATT callback confirms connection)
        updateDeviceConnectionState(targetMac, NearbyConnectionState.REQUESTING)

        handler.removeCallbacks(connectionTimeoutRunnable)
        handler.postDelayed(connectionTimeoutRunnable, CONNECTION_TIMEOUT_MS)

        gattClient.connectToDevice(targetMac)
    }
    
    fun disconnect(target: String) {
        val device = findDevice(target)
        val targetMac = device?.macAddress ?: target
        disconnectInternal(targetMac)
        if (isSessionActive && !isBurstActive && !hasActiveConnection()) {
            advertiser.stopAdvertising()
            handler.removeCallbacks(advertiseRunnable)
            handler.post(advertiseRunnable)
        }
    }
    
    fun acceptIncomingConnection(macAddress: String) {
        val device = findDevice(macAddress)
        val targetMac = device?.macAddress ?: macAddress

        // Single Active Peer Rule: If connected to another peer, disconnect it completely first
        val currentActive = getCurrentActivePeer()
        if (currentActive != null && !currentActive.equals(targetMac, ignoreCase = true)) {
            Log.d(TAG, "Single active peer rule: Disconnecting existing peer $currentActive before accepting $targetMac")
            disconnectInternal(currentActive)
        }

        if (device?.connectionState == NearbyConnectionState.CONNECTED && activeConnectedPeerMac.equals(targetMac, ignoreCase = true)) {
            Log.d(TAG, "Incoming connection for $targetMac already connected. Skipping duplicate accept.")
            return
        }

        activeConnectedPeerMac = targetMac
        updateDeviceConnectionState(targetMac, NearbyConnectionState.CONNECTED)
        scanner.enforceSingleConnectedPeer(targetMac)
        gattServer.acceptConnection(targetMac)
        // Stop presence advertising once connection is accepted to avoid BLE radio interference
        advertiser.stopAdvertising()
        isBurstActive = false
    }
    
    fun declineIncomingConnection(macAddress: String) {
        val device = findDevice(macAddress)
        val targetMac = device?.macAddress ?: macAddress
        updateDeviceConnectionState(targetMac, NearbyConnectionState.DISCONNECTED)
        gattServer.declineConnection(targetMac)
    }

    fun sendNearbyPayload(target: String, payload: String): Boolean {
        val device = findDevice(target)
        val targetMac = device?.macAddress ?: target
        val stableId = device?.id ?: target
        val payloadByteLength = payload.toByteArray(Charsets.UTF_8).size

        Log.d(
            TAG,
            "NEARBY_BLE: sendNearbyPayload requested for target=$target (resolvedMac=$targetMac, stableId=$stableId, bytes=$payloadByteLength, state=${device?.connectionState}, serverActive=${gattServer.hasActiveConnections()}, clientActive=${gattClient.isConnected})"
        )

        // 1. Try server notification first (if this phone accepted incoming connection from target)
        var success = gattServer.sendPayloadNotification(targetMac, payload)
        
        // 2. If server notification didn't send (e.g. this phone is in client role), try client GATT write
        if (!success) {
            success = gattClient.sendPayload(payload)
        }
        
        if (success) {
            Log.d(TAG, "NEARBY_BLE: Payload sent successfully to $targetMac (stableId=$stableId)")
        } else {
            Log.e(TAG, "NEARBY_BLE: Failed to send payload to $targetMac (stableId=$stableId)")
        }
        return success
    }

    private val advertiseRunnable = object : Runnable {
        override fun run() {
            if (!isSessionActive || currentIntervalMs <= 0) return
            
            // While a GATT connection is active or pending, do NOT start presence advertising bursts
            if (hasActiveConnection() || isConnectionPending) {
                handler.postDelayed(this, currentIntervalMs)
                return
            }

            // Expose presence for a short burst (e.g., 2 seconds)
            isBurstActive = true
            advertiser.startAdvertising()
            
            handler.postDelayed({
                isBurstActive = false
                if (isSessionActive) {
                    // Do NOT stop advertising if a connection became active or pending
                    if (!hasActiveConnection() && !isConnectionPending) {
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
        // Start single scanner session owned by the service/manager
        scanner.startScanning()
    }

    private fun stopPresenceSession() {
        isSessionActive = false
        isBurstActive = false
        isConnectionPending = false
        pendingConnectionMac = null
        activeConnectedPeerMac = null
        scanner.enforceSingleConnectedPeer(null)
        handler.removeCallbacks(connectionTimeoutRunnable)
        handler.removeCallbacks(advertiseRunnable)
        advertiser.stopAdvertising()
        scanner.stopScanning()
        gattServer.stopServer()
    }

    fun startAdvertisingPresence() {
        gattServer.startServer()
        advertiser.startAdvertising()
    }

    fun stopAdvertisingPresence() {
        if (!isSessionActive && !isConnectionPending && !hasActiveConnection()) {
            advertiser.stopAdvertising()
        }
    }

    fun startScanningForNearby() {
        scanner.startScanning()
    }

    fun stopScanningForNearby() {
        if (!isSessionActive) {
            scanner.stopScanning()
        }
    }

    private var isManualRefreshing = false
    private val refreshLock = Any()

    fun manualRefresh(onComplete: () -> Unit = {}) {
        synchronized(refreshLock) {
            if (isManualRefreshing) {
                Log.d(TAG, "Manual refresh already running, ignoring duplicate request.")
                return
            }
            isManualRefreshing = true
        }

        Log.d(TAG, "Triggering fresh nearby BLE discovery...")
        scanner.manualRefresh()

        // If presence discovery session is active and not connected, perform an active burst
        if (isSessionActive && !hasActiveConnection()) {
            advertiser.startAdvertising()
        }

        handler.postDelayed({
            synchronized(refreshLock) {
                isManualRefreshing = false
            }
            if (isSessionActive && !hasActiveConnection() && !isBurstActive) {
                advertiser.stopAdvertising()
            }
            Log.d(TAG, "Manual refresh finished.")
            onComplete()
        }, 1500L)
    }
}
