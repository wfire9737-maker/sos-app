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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class NearbyDeviceScanner(private val context: Context) {
    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager?.adapter
    private val scanner: BluetoothLeScanner? = bluetoothAdapter?.bluetoothLeScanner
    private var isScanning = false

    private val _nearbyDevices = MutableStateFlow<Map<String, NearbyDevice>>(emptyMap())
    val nearbyDevices: StateFlow<Map<String, NearbyDevice>> = _nearbyDevices.asStateFlow()
    private val deviceMapLock = Any()

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            // 1. Verify NEARBY_SERVICE_UUID exactly as existing implementation does
            val serviceUuids = result?.scanRecord?.serviceUuids
            if (serviceUuids?.contains(ParcelUuid(NearbyBleProtocol.NEARBY_SERVICE_UUID)) != true) {
                return
            }

            result?.device?.let { device ->
                try {
                    // 2. Determine a stable device identifier (Bluetooth device address / MAC)
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
                        // Preserve existing stale-device cleanup: remove devices older than 60s unless connected/requesting
                        val cutoff = timestamp - 60_000L
                        val updatedMap = _nearbyDevices.value.filterValues {
                            it.lastSeen >= cutoff || it.connectionState != NearbyConnectionState.DISCONNECTED
                        }.toMutableMap()

                        // 3. Check whether that identifier already exists in nearbyDevices
                        val existingDevice = updatedMap[address]
                        if (existingDevice != null) {
                            // 4. If it exists:
                            //    - update RSSI
                            //    - update lastSeen
                            //    - update Nearby device name if advertisement contains a newer name
                            //    - preserve connection state
                            //    - DO NOT create another list entry
                            val updatedName = if (!discoveredName.isNullOrBlank()) discoveredName else existingDevice.deviceName
                            updatedMap[address] = existingDevice.copy(
                                rssi = rssi,
                                lastSeen = timestamp,
                                deviceName = updatedName,
                                connectionState = existingDevice.connectionState // explicitly preserved
                            )
                            Log.d("NearbyScanner", "Updated existing nearby device: $address ($updatedName) RSSI=$rssi State=${existingDevice.connectionState}")
                        } else {
                            // 5. If it does not exist:
                            //    - create exactly one NearbyDevice entry
                            val initialName = if (!discoveredName.isNullOrBlank()) discoveredName else NearbyBleProtocol.DEFAULT_DEVICE_NAME
                            updatedMap[address] = NearbyDevice(
                                macAddress = address,
                                deviceName = initialName,
                                lastSeen = timestamp,
                                rssi = rssi,
                                connectionState = NearbyConnectionState.DISCONNECTED
                            )
                            Log.d("NearbyScanner", "Discovered new nearby device: $address ($initialName) RSSI=$rssi")
                        }

                        _nearbyDevices.value = updatedMap
                    }
                } catch (e: SecurityException) {
                    Log.e("NearbyScanner", "SecurityException during scan", e)
                }
            }
        }

        override fun onScanFailed(errorCode: Int) {
            isScanning = false
            Log.e("NearbyScanner", "Scan failed with error code: $errorCode")
        }
    }

    fun startScanning() {
        if (isScanning) return
        try {
            if (scanner == null) {
                Log.w("NearbyScanner", "Bluetooth LE Scanner not available.")
                return
            }

            val filter = ScanFilter.Builder()
                .setServiceUuid(ParcelUuid(NearbyBleProtocol.NEARBY_SERVICE_UUID))
                .build()

            val settings = ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .build()

            scanner.startScan(listOf(filter), settings, scanCallback)
            isScanning = true
            Log.d("NearbyScanner", "Started scanning for nearby presence.")
        } catch (e: SecurityException) {
            Log.e("NearbyScanner", "Missing BLUETOOTH_SCAN permission", e)
        }
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
        }
    }
    
    fun stopScanning() {
        if (!isScanning) return
        try {
            scanner?.stopScan(scanCallback)
            isScanning = false
            Log.d("NearbyScanner", "Stopped scanning for nearby presence.")
        } catch (e: SecurityException) {
            Log.e("NearbyScanner", "Missing BLUETOOTH_SCAN permission", e)
        }
    }
}
