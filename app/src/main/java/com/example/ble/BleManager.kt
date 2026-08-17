package com.example.ble

import android.annotation.SuppressLint
import android.bluetooth.*
import android.bluetooth.le.*
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

@SuppressLint("MissingPermission")
class BleManager(private val context: Context) {

    enum class BleState {
        DISCONNECTED, SCANNING, CONNECTING, CONNECTED, DISCOVERING_SERVICES, READY, ERROR
    }

    private val _connectionState = MutableStateFlow(BleState.DISCONNECTED)
    val connectionState: StateFlow<BleState> = _connectionState.asStateFlow()

    private val _batteryLevel = MutableStateFlow<Int?>(null)
    val batteryLevel: StateFlow<Int?> = _batteryLevel.asStateFlow()
    private val _rssi = MutableStateFlow<Int?>(null)
    val rssi: StateFlow<Int?> = _rssi.asStateFlow()
    private val _deviceName = MutableStateFlow<String?>("ESP32 SOS Band")
    val deviceName: StateFlow<String?> = _deviceName.asStateFlow()
    private val _deviceMac = MutableStateFlow<String?>("00:00:00:00:00:00")
    val deviceMac: StateFlow<String?> = _deviceMac.asStateFlow()

    private val _sosEvent = MutableStateFlow<Boolean>(false)
    val sosEvent: StateFlow<Boolean> = _sosEvent.asStateFlow()

    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager?.adapter
    private val scanner: BluetoothLeScanner? = bluetoothAdapter?.bluetoothLeScanner

    private var gatt: BluetoothGatt? = null
    private var sosCharacteristic: BluetoothGattCharacteristic? = null
    private var commandCharacteristic: BluetoothGattCharacteristic? = null

    private var isScanning = false
    private val handler = Handler(Looper.getMainLooper())

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            result?.device?.let { device ->
                try {
                    if (device.name?.contains("PhysicalSOS-ESP32", true) == true || device.name?.contains("ESP32-SOS", true) == true) {
                        stopScan()
                        connectToDevice(device)
                    }
                } catch (e: SecurityException) {
                    Log.e("BleManager", "SecurityException in scanCallback", e)
                }
            }
        }
    }

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            try {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    if (newState == BluetoothProfile.STATE_CONNECTED) {
                        _deviceName.value = gatt.device.name ?: "Unknown ESP32"
                        _deviceMac.value = gatt.device.address ?: "00:00:00:00:00:00"
                        _connectionState.value = BleState.CONNECTED
                        _connectionState.value = BleState.DISCOVERING_SERVICES
                        gatt.discoverServices()
                    } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                        disconnect()
                    }
                } else {
                    _connectionState.value = BleState.ERROR
                    disconnect()
                }
            } catch (e: SecurityException) {
                Log.e("BleManager", "Missing BLUETOOTH_CONNECT permission", e)
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            try {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    val service = gatt.getService(BleProtocol.SERVICE_UUID)
                    if (service != null) {
                        sosCharacteristic = service.getCharacteristic(BleProtocol.SOS_CHARACTERISTIC_UUID)
                        commandCharacteristic = service.getCharacteristic(BleProtocol.COMMAND_CHARACTERISTIC_UUID)

                        sosCharacteristic?.let {
                            gatt.setCharacteristicNotification(it, true)
                            val descriptor = it.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"))
                            if (descriptor != null) {
                                descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                                gatt.writeDescriptor(descriptor)
                            }
                        }
                    }
                    
                    // Look for Battery characteristic in all services
                    for (srv in gatt.services) {
                        val batChar = srv.getCharacteristic(BleProtocol.BATTERY_CHARACTERISTIC_UUID)
                        if (batChar != null) {
                            gatt.setCharacteristicNotification(batChar, true)
                            val desc = batChar.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"))
                            if (desc != null) {
                                desc.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                                gatt.writeDescriptor(desc)
                            }
                            gatt.readCharacteristic(batChar)
                        }
                    }

                    _connectionState.value = BleState.READY
                    
                    // Trigger a recurring RSSI read
                    readRssiPeriodically()
                } else {
                    _connectionState.value = BleState.ERROR
                }
            } catch (e: SecurityException) {
                Log.e("BleManager", "Missing BLUETOOTH_CONNECT permission", e)
            }
        }

        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            if (status == BluetoothGatt.GATT_SUCCESS && characteristic.uuid == BleProtocol.BATTERY_CHARACTERISTIC_UUID) {
                val value = characteristic.value
                if (value != null && value.isNotEmpty()) {
                    _batteryLevel.value = value[0].toInt()
                }
            }
        }

        override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
            if (characteristic.uuid == BleProtocol.SOS_CHARACTERISTIC_UUID) {
                val value = characteristic.value
                val isSos = value != null && value.isNotEmpty() && value[0].toInt() != 0
                _sosEvent.value = isSos
            } else if (characteristic.uuid == BleProtocol.BATTERY_CHARACTERISTIC_UUID) {
                val value = characteristic.value
                if (value != null && value.isNotEmpty()) {
                    _batteryLevel.value = value[0].toInt()
                }
            }
        }
        
        override fun onReadRemoteRssi(gatt: BluetoothGatt, rssi: Int, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                _rssi.value = rssi
            }
        }
    }

    fun isBluetoothEnabled(): Boolean {
        return try {
            bluetoothAdapter?.isEnabled == true
        } catch (e: SecurityException) {
            Log.e("BleManager", "Bluetooth permission denied", e)
            false
        }
    }

    fun scanAndConnect() {
        try {
            if (!isBluetoothEnabled() || isScanning || _connectionState.value == BleState.CONNECTED || _connectionState.value == BleState.READY) return
            
            _connectionState.value = BleState.SCANNING
            isScanning = true
            
            val filter = ScanFilter.Builder().build()
            val settings = ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build()
            
            scanner?.startScan(listOf(filter), settings, scanCallback)
            
            handler.postDelayed({
                if (isScanning) {
                    stopScan()
                    if (_connectionState.value == BleState.SCANNING) {
                        _connectionState.value = BleState.ERROR
                    }
                }
            }, 10000)
        } catch (e: SecurityException) {
            Log.e("BleManager", "Missing BLUETOOTH_SCAN permission", e)
            _connectionState.value = BleState.ERROR
        }
    }

    private fun stopScan() {
        try {
            if (isScanning) {
                scanner?.stopScan(scanCallback)
                isScanning = false
            }
        } catch (e: SecurityException) {
            Log.e("BleManager", "Missing BLUETOOTH_SCAN permission", e)
        }
    }

    private fun connectToDevice(device: BluetoothDevice) {
        try {
            _connectionState.value = BleState.CONNECTING
            gatt = device.connectGatt(context, false, gattCallback)
        } catch (e: SecurityException) {
            Log.e("BleManager", "Missing BLUETOOTH_CONNECT permission", e)
            _connectionState.value = BleState.ERROR
        }
    }

    private fun readRssiPeriodically() {
        handler.postDelayed(object : Runnable {
            override fun run() {
                try {
                    if (_connectionState.value == BleState.READY || _connectionState.value == BleState.CONNECTED) {
                        gatt?.readRemoteRssi()
                        handler.postDelayed(this, 5000)
                    }
                } catch (e: SecurityException) {
                    Log.e("BleManager", "SecurityException reading RSSI", e)
                }
            }
        }, 2000)
    }

    fun disconnect() {
        try {
            stopScan()
            gatt?.disconnect()
            gatt?.close()
            gatt = null
            sosCharacteristic = null
            commandCharacteristic = null
            _connectionState.value = BleState.DISCONNECTED
            _batteryLevel.value = null
            _rssi.value = null
        } catch (e: SecurityException) {
            Log.e("BleManager", "Missing BLUETOOTH_CONNECT permission", e)
        }
    }

    fun sendCommand(command: String) {
        try {
            if (_connectionState.value == BleState.READY) {
                commandCharacteristic?.let {
                    it.value = command.toByteArray()
                    gatt?.writeCharacteristic(it)
                }
            }
        } catch (e: SecurityException) {
            Log.e("BleManager", "Missing BLUETOOTH_CONNECT permission", e)
        }
    }
}
