package com.example.ble.nearby

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.ParcelUuid
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class NearbyDeviceScanner(private val context: Context) {
    companion object {
        private const val TAG = "NearbyScanner"
        const val DISCOVERY_WINDOW_MS = 5000L
    }

    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager?.adapter
    private val scanner: BluetoothLeScanner? = bluetoothAdapter?.bluetoothLeScanner
    
    private var isHardwareScanning = false
    private var isDiscoverySessionActive = false
    private var discoveryCycleJob: Job? = null
    private val scannerScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    // UI-exposed list of discovered devices
    private val _nearbyDevices = MutableStateFlow<Map<String, NearbyDevice>>(emptyMap())
    val nearbyDevices: StateFlow<Map<String, NearbyDevice>> = _nearbyDevices.asStateFlow()

    // Internal buffer for collecting scan results during the current 5-second window
    private val temporaryDiscoveredDevices = mutableMapOf<String, NearbyDevice>()
    private val deviceMapLock = Any()

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            // Verify NEARBY_SERVICE_UUID
            val serviceUuids = result?.scanRecord?.serviceUuids
            if (serviceUuids?.contains(ParcelUuid(NearbyBleProtocol.NEARBY_SERVICE_UUID)) != true) {
                return
            }

            result?.device?.let { device ->
                try {
                    val address = device.address ?: return
                    val rssi = result.rssi
                    val timestamp = System.currentTimeMillis()
                    
                    // Extract Nearby-specific advertised name from service data if available
                    var discoveredName: String? = null
                    val nameServiceData = result.scanRecord?.getServiceData(ParcelUuid(NearbyBleProtocol.NEARBY_NAME_SERVICE_UUID))
                    if (nameServiceData != null && nameServiceData.isNotEmpty()) {
                        try {
                            val decoded = String(nameServiceData, Charsets.UTF_8).trim()
                            if (decoded.isNotBlank()) {
                                discoveredName = decoded
                            }
                        } catch (e: Exception) {}
                    }

                    synchronized(deviceMapLock) {
                        val existingInBatch = temporaryDiscoveredDevices[address]
                        val existingInUi = _nearbyDevices.value[address]
                        val resolvedName = if (!discoveredName.isNullOrBlank()) {
                            discoveredName
                        } else {
                            existingInBatch?.deviceName ?: existingInUi?.deviceName ?: NearbyBleProtocol.DEFAULT_DEVICE_NAME
                        }

                        val connectionState = existingInUi?.connectionState ?: NearbyConnectionState.DISCONNECTED

                        // Deduplicate: Exactly ONE NearbyDevice entry per stable BLE MAC address
                        temporaryDiscoveredDevices[address] = NearbyDevice(
                            macAddress = address,
                            deviceName = resolvedName,
                            lastSeen = timestamp,
                            rssi = rssi,
                            connectionState = connectionState
                        )
                    }
                } catch (e: SecurityException) {
                    Log.e(TAG, "SecurityException during scan", e)
                }
            }
        }

        override fun onScanFailed(errorCode: Int) {
            isHardwareScanning = false
            Log.e(TAG, "Scan failed with error code: $errorCode")
        }
    }

    private fun startBleHardwareScan() {
        if (isHardwareScanning) return
        try {
            if (scanner == null) {
                Log.w(TAG, "Bluetooth LE Scanner not available.")
                return
            }

            val filter = ScanFilter.Builder()
                .setServiceUuid(ParcelUuid(NearbyBleProtocol.NEARBY_SERVICE_UUID))
                .build()

            val settings = ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .build()

            scanner.startScan(listOf(filter), settings, scanCallback)
            isHardwareScanning = true
            Log.d(TAG, "Started 5-second BLE hardware scan.")
        } catch (e: SecurityException) {
            Log.e(TAG, "Missing BLUETOOTH_SCAN permission", e)
        }
    }

    private fun stopBleHardwareScan() {
        if (!isHardwareScanning) return
        try {
            scanner?.stopScan(scanCallback)
            isHardwareScanning = false
            Log.d(TAG, "Stopped 5-second BLE hardware scan.")
        } catch (e: SecurityException) {
            Log.e(TAG, "Missing BLUETOOTH_SCAN permission", e)
        }
    }

    fun startScanning() {
        if (isDiscoverySessionActive) return
        isDiscoverySessionActive = true
        Log.d(TAG, "Starting controlled 5-second discovery refresh cycle.")

        discoveryCycleJob?.cancel()
        discoveryCycleJob = scannerScope.launch {
            while (isActive && isDiscoverySessionActive) {
                // 1. Clear temporary discovery buffer for the new 5-second cycle
                synchronized(deviceMapLock) {
                    temporaryDiscoveredDevices.clear()
                }

                // 2. Start BLE hardware scan
                startBleHardwareScan()

                // 3. Collect scan results during the 5-second window
                delay(DISCOVERY_WINDOW_MS)

                // 4. Stop BLE hardware scan
                stopBleHardwareScan()

                // 5. Build fresh deduplicated snapshot while protecting active/requesting connections
                synchronized(deviceMapLock) {
                    val freshMap = mutableMapOf<String, NearbyDevice>()

                    // Add all devices discovered in the current 5-second window
                    freshMap.putAll(temporaryDiscoveredDevices)

                    // Preserve any currently CONNECTED or REQUESTING device
                    _nearbyDevices.value.forEach { (mac, existingDevice) ->
                        if (existingDevice.connectionState != NearbyConnectionState.DISCONNECTED) {
                            if (!freshMap.containsKey(mac)) {
                                freshMap[mac] = existingDevice
                            } else {
                                freshMap[mac] = freshMap[mac]!!.copy(
                                    connectionState = existingDevice.connectionState
                                )
                            }
                        }
                    }

                    // 6. Replace the UI's discovered-device StateFlow with the fresh snapshot
                    _nearbyDevices.value = freshMap
                    Log.d(TAG, "Discovery cycle complete: ${_nearbyDevices.value.size} active devices.")
                }
            }
        }
    }

    fun stopScanning() {
        isDiscoverySessionActive = false
        discoveryCycleJob?.cancel()
        discoveryCycleJob = null
        stopBleHardwareScan()
        synchronized(deviceMapLock) {
            temporaryDiscoveredDevices.clear()
        }
        Log.d(TAG, "Stopped nearby discovery session.")
    }

    fun updateDeviceConnectionState(macAddress: String, state: NearbyConnectionState, deviceName: String? = null) {
        synchronized(deviceMapLock) {
            val updatedMap = _nearbyDevices.value.toMutableMap()
            val existing = updatedMap[macAddress]
            val resolvedName = deviceName ?: existing?.deviceName ?: NearbyBleProtocol.DEFAULT_DEVICE_NAME
            val updatedDevice = existing?.copy(
                connectionState = state,
                deviceName = resolvedName
            ) ?: NearbyDevice(
                macAddress = macAddress,
                deviceName = resolvedName,
                lastSeen = System.currentTimeMillis(),
                rssi = -50,
                connectionState = state
            )
            updatedMap[macAddress] = updatedDevice
            _nearbyDevices.value = updatedMap

            if (temporaryDiscoveredDevices.containsKey(macAddress)) {
                temporaryDiscoveredDevices[macAddress] = temporaryDiscoveredDevices[macAddress]!!.copy(
                    connectionState = state,
                    deviceName = resolvedName
                )
            }
        }
    }
}
