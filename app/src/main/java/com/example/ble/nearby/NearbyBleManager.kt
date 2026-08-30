package com.example.ble.nearby

import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NearbyBleManager @Inject constructor(
    private val advertiser: NearbyPresenceAdvertiser,
    private val scanner: NearbyDeviceScanner,
    private val gattServer: NearbyGattServer,
    private val gattClient: NearbyGattClient
) {
    val nearbyDevices: StateFlow<Map<String, NearbyDevice>> = scanner.nearbyDevices
    
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
        scanner.updateDeviceConnectionState(macAddress, state)
    }
    
    fun requestConnection(macAddress: String) {
        gattClient.connectToDevice(macAddress)
    }
    
    fun disconnect(macAddress: String) {
        gattClient.disconnect()
        updateDeviceConnectionState(macAddress, NearbyConnectionState.DISCONNECTED)
    }
    
    fun acceptIncomingConnection(macAddress: String) {
        gattServer.acceptConnection(macAddress)
    }
    
    fun declineIncomingConnection(macAddress: String) {
        gattServer.declineConnection(macAddress)
    }
    private var currentIntervalMs: Long = 0L
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
