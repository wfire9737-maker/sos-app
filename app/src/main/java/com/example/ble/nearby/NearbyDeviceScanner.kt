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
        const val STALE_DEVICE_THRESHOLD_MS = 60_000L // 60-second stale retention window
        private const val PRUNE_INTERVAL_MS = 5_000L // 5-second periodic stale cleanup check
    }

    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter?
        get() = bluetoothManager?.adapter
    private val scanner: BluetoothLeScanner?
        get() = bluetoothAdapter?.bluetoothLeScanner

    private var isHardwareScanning = false
    private var isDiscoverySessionActive = false
    private var stalePruningJob: Job? = null
    private val scannerScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    // Internal cache of discovered devices keyed by application-level stableDeviceId
    private val deviceCache = mutableMapOf<String, NearbyDevice>()
    private val deviceMapLock = Any()

    // UI-exposed map of discovered devices (Key: stableDeviceId, Value: NearbyDevice)
    private val _nearbyDevices = MutableStateFlow<Map<String, NearbyDevice>>(emptyMap())
    val nearbyDevices: StateFlow<Map<String, NearbyDevice>> = _nearbyDevices.asStateFlow()

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

                    var parsedStableId: String? = null
                    var parsedName: String? = null

                    // Extract payload: "${stableDeviceId}:${deviceName}" from service data
                    val nameServiceData = result.scanRecord?.getServiceData(ParcelUuid(NearbyBleProtocol.NEARBY_NAME_SERVICE_UUID))
                    if (nameServiceData != null && nameServiceData.isNotEmpty()) {
                        try {
                            val decoded = String(nameServiceData, Charsets.UTF_8).trim()
                            if (decoded.contains(":")) {
                                val parts = decoded.split(":", limit = 2)
                                if (parts[0].isNotBlank()) {
                                    parsedStableId = parts[0].trim()
                                }
                                if (parts.size > 1 && parts[1].isNotBlank()) {
                                    parsedName = parts[1].trim()
                                }
                            } else if (decoded.isNotBlank()) {
                                parsedName = decoded
                            }
                        } catch (_: Exception) {}
                    }

                    // Use stableDeviceId as the primary unique key, falling back to BLE address only if missing
                    val stableDeviceId = parsedStableId ?: address

                    synchronized(deviceMapLock) {
                        val existing = deviceCache[stableDeviceId]
                        val resolvedName = if (!parsedName.isNullOrBlank()) {
                            parsedName
                        } else {
                            existing?.deviceName ?: NearbyBleProtocol.DEFAULT_DEVICE_NAME
                        }

                        val connectionState = existing?.connectionState ?: NearbyConnectionState.DISCONNECTED

                        val updatedDevice = NearbyDevice(
                            id = stableDeviceId,
                            macAddress = address, // Always keep the freshest BLE MAC for GATT connection
                            deviceName = resolvedName,
                            lastSeen = timestamp,
                            rssi = rssi,
                            connectionState = connectionState
                        )

                        deviceCache[stableDeviceId] = updatedDevice
                        _nearbyDevices.value = deviceCache.toMap()

                        if (existing == null) {
                            Log.d(
                                TAG,
                                "NEARBY_DEBUG: [NEW_ENTRY] stableDeviceId=$stableDeviceId address=$address name=$resolvedName rssi=$rssi"
                            )
                        } else {
                            Log.d(
                                TAG,
                                "NEARBY_DEBUG: [UPDATE_ENTRY] stableDeviceId=$stableDeviceId address=$address name=$resolvedName rssi=$rssi"
                            )
                        }
                    }
                } catch (e: SecurityException) {
                    Log.e(TAG, "NEARBY_DEBUG: SecurityException during scan result processing", e)
                } catch (e: Exception) {
                    Log.e(TAG, "NEARBY_DEBUG: Error processing scan result", e)
                }
            }
        }

        override fun onScanFailed(errorCode: Int) {
            isHardwareScanning = false
            Log.e(TAG, "NEARBY_DEBUG: Scan failure callback errorCode=$errorCode")
        }
    }

    fun startScanning() {
        if (isDiscoverySessionActive) {
            Log.d(TAG, "NEARBY_DEBUG: Scanner already running")
            return
        }

        val adapter = bluetoothAdapter
        if (adapter == null || !adapter.isEnabled) {
            Log.w(TAG, "NEARBY_DEBUG: Bluetooth adapter unavailable or disabled. Cannot start scan.")
            return
        }

        val leScanner = scanner
        if (leScanner == null) {
            Log.w(TAG, "NEARBY_DEBUG: BluetoothLeScanner not available.")
            return
        }

        try {
            val filter = ScanFilter.Builder()
                .setServiceUuid(ParcelUuid(NearbyBleProtocol.NEARBY_SERVICE_UUID))
                .build()

            val settings = ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_BALANCED)
                .build()

            leScanner.startScan(listOf(filter), settings, scanCallback)
            isHardwareScanning = true
            isDiscoverySessionActive = true
            Log.d(TAG, "NEARBY_DEBUG: Scanner started (single continuous session, mode=BALANCED)")

            startPeriodicStalePruning()
        } catch (e: SecurityException) {
            Log.e(TAG, "NEARBY_DEBUG: Missing BLUETOOTH_SCAN permission to start scan", e)
            isDiscoverySessionActive = false
            isHardwareScanning = false
        } catch (e: Exception) {
            Log.e(TAG, "NEARBY_DEBUG: Failed to start BLE scan", e)
            isDiscoverySessionActive = false
            isHardwareScanning = false
        }
    }

    fun stopScanning() {
        if (!isDiscoverySessionActive && !isHardwareScanning) {
            return
        }

        isDiscoverySessionActive = false
        stalePruningJob?.cancel()
        stalePruningJob = null

        if (isHardwareScanning) {
            try {
                scanner?.stopScan(scanCallback)
                Log.d(TAG, "NEARBY_DEBUG: Scanner stopped")
            } catch (e: SecurityException) {
                Log.e(TAG, "NEARBY_DEBUG: Missing BLUETOOTH_SCAN permission to stop scan", e)
            } catch (e: Exception) {
                Log.e(TAG, "NEARBY_DEBUG: Error stopping BLE scan", e)
            } finally {
                isHardwareScanning = false
            }
        }
    }

    private fun startPeriodicStalePruning() {
        stalePruningJob?.cancel()
        stalePruningJob = scannerScope.launch {
            while (isActive && isDiscoverySessionActive) {
                delay(PRUNE_INTERVAL_MS)
                pruneStaleDevices()
            }
        }
    }

    private fun pruneStaleDevices() {
        val now = System.currentTimeMillis()
        var hasPruned = false

        synchronized(deviceMapLock) {
            val iterator = deviceCache.entries.iterator()
            while (iterator.hasNext()) {
                val entry = iterator.next()
                val device = entry.value

                // Never prune devices with active or pending connections
                if (device.connectionState == NearbyConnectionState.DISCONNECTED) {
                    if (now - device.lastSeen > STALE_DEVICE_THRESHOLD_MS) {
                        iterator.remove()
                        hasPruned = true
                        Log.d(
                            TAG,
                            "NEARBY_DEBUG: [STALE_REMOVE] stableDeviceId=${device.id} address=${device.macAddress} name=${device.deviceName} (inactive for >60s)"
                        )
                    }
                }
            }

            if (hasPruned) {
                _nearbyDevices.value = deviceCache.toMap()
            }
        }
    }

    fun updateDeviceConnectionState(identifier: String, state: NearbyConnectionState, deviceName: String? = null) {
        synchronized(deviceMapLock) {
            // Find existing device by stableDeviceId OR BLE macAddress
            val existingEntry = deviceCache.values.firstOrNull { it.id == identifier || it.macAddress == identifier }
            val resolvedKey = existingEntry?.id ?: identifier
            val resolvedMac = existingEntry?.macAddress ?: identifier
            val resolvedName = deviceName ?: existingEntry?.deviceName ?: NearbyBleProtocol.DEFAULT_DEVICE_NAME

            val updatedDevice = existingEntry?.copy(
                connectionState = state,
                deviceName = resolvedName
            ) ?: NearbyDevice(
                id = resolvedKey,
                macAddress = resolvedMac,
                deviceName = resolvedName,
                lastSeen = System.currentTimeMillis(),
                rssi = -50,
                connectionState = state
            )

            deviceCache[resolvedKey] = updatedDevice
            _nearbyDevices.value = deviceCache.toMap()
        }
    }
}
