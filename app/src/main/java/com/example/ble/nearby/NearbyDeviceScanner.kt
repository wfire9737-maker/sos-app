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

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            result?.device?.let { device ->
                try {
                    val address = device.address
                    val rssi = result.rssi
                    val timestamp = System.currentTimeMillis()
                    
                    val updatedMap = _nearbyDevices.value.toMutableMap()
                    val existingDevice = updatedMap[address]
                    val currentState = existingDevice?.connectionState ?: NearbyConnectionState.DISCONNECTED
                    
                    val newDevice = NearbyDevice(macAddress = address, lastSeen = timestamp, rssi = rssi, connectionState = currentState)
                    
                    updatedMap[address] = newDevice
                    _nearbyDevices.value = updatedMap
                    
                    Log.d("NearbyScanner", "Discovered nearby device: $address with RSSI $rssi")
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

    fun updateDeviceConnectionState(macAddress: String, state: NearbyConnectionState) {
        val updatedMap = _nearbyDevices.value.toMutableMap()
        val device = updatedMap[macAddress]
        if (device != null) {
            updatedMap[macAddress] = device.copy(connectionState = state)
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
