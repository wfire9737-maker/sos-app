package com.example.ble.nearby

import android.bluetooth.*
import android.content.Context
import android.os.Build
import android.util.Log

class NearbyGattClient(private val context: Context) {
    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager?.adapter
    
    private var bluetoothGatt: BluetoothGatt? = null
    var isConnected: Boolean = false
        private set
    var connectedDeviceAddress: String? = null
        private set
    var onConnectionStateChanged: ((String, NearbyConnectionState) -> Unit)? = null
    var onIncomingPayloadReceived: ((String, String) -> Unit)? = null

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            super.onConnectionStateChange(gatt, status, newState)
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.d("NearbyGattClient", "NEARBY_BLE: Connected to GATT server ${gatt.device.address}. Requesting MTU 512...")
                try {
                    val mtuRequested = gatt.requestMtu(512)
                    if (!mtuRequested) {
                        Log.d("NearbyGattClient", "NEARBY_BLE: MTU request failed synchronously, discovering services...")
                        gatt.discoverServices()
                    }
                } catch (e: SecurityException) {
                    Log.d("NearbyGattClient", "Security Exception on MTU request, discovering services...")
                    try {
                        gatt.discoverServices()
                    } catch (_: SecurityException) {}
                }
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.d("NearbyGattClient", "NEARBY_BLE: Disconnected from GATT server ${gatt.device.address}.")
                isConnected = false
                connectedDeviceAddress = null
                onConnectionStateChanged?.invoke(gatt.device.address, NearbyConnectionState.DISCONNECTED)
                closeGatt()
            }
        }

        override fun onMtuChanged(gatt: BluetoothGatt, mtu: Int, status: Int) {
            super.onMtuChanged(gatt, mtu, status)
            Log.d("NearbyGattClient", "NEARBY_BLE: MTU changed to $mtu. Discovering services...")
            try {
                gatt.discoverServices()
            } catch (_: SecurityException) {}
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            super.onServicesDiscovered(gatt, status)
            if (status == BluetoothGatt.GATT_SUCCESS) {
                try {
                    val service = gatt.getService(NearbyBleProtocol.NEARBY_SERVICE_UUID)
                    val statusChar = service?.getCharacteristic(NearbyBleProtocol.CONNECTION_STATUS_CHAR_UUID)
                    val requestChar = service?.getCharacteristic(NearbyBleProtocol.CONNECTION_REQUEST_CHAR_UUID)
                    val payloadChar = service?.getCharacteristic(NearbyBleProtocol.NEARBY_PAYLOAD_CHAR_UUID)
                    
                    if (payloadChar != null) {
                        gatt.setCharacteristicNotification(payloadChar, true)
                        val payloadConfigDesc = payloadChar.getDescriptor(java.util.UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"))
                        if (payloadConfigDesc != null) {
                            payloadConfigDesc.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                            gatt.writeDescriptor(payloadConfigDesc)
                            return // Chain continues in onDescriptorWrite
                        }
                    }
                    
                    // Fallback if payload char missing or descriptor missing
                    if (statusChar != null && requestChar != null) {
                        gatt.setCharacteristicNotification(statusChar, true)
                        val configDesc = statusChar.getDescriptor(java.util.UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"))
                        if (configDesc != null) {
                            configDesc.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                            gatt.writeDescriptor(configDesc)
                        } else {
                            sendConnectionRequest(gatt, requestChar)
                        }
                    }
                } catch (_: SecurityException) {}
            }
        }

        override fun onDescriptorWrite(gatt: BluetoothGatt, descriptor: BluetoothGattDescriptor, status: Int) {
            super.onDescriptorWrite(gatt, descriptor, status)
            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (descriptor.characteristic.uuid == NearbyBleProtocol.NEARBY_PAYLOAD_CHAR_UUID) {
                    try {
                        val service = gatt.getService(NearbyBleProtocol.NEARBY_SERVICE_UUID)
                        val statusChar = service?.getCharacteristic(NearbyBleProtocol.CONNECTION_STATUS_CHAR_UUID)
                        val requestChar = service?.getCharacteristic(NearbyBleProtocol.CONNECTION_REQUEST_CHAR_UUID)
                        if (statusChar != null && requestChar != null) {
                            gatt.setCharacteristicNotification(statusChar, true)
                            val configDesc = statusChar.getDescriptor(java.util.UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"))
                            if (configDesc != null) {
                                configDesc.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                                gatt.writeDescriptor(configDesc)
                            } else {
                                sendConnectionRequest(gatt, requestChar)
                            }
                        }
                    } catch (_: SecurityException) {}
                } else if (descriptor.characteristic.uuid == NearbyBleProtocol.CONNECTION_STATUS_CHAR_UUID) {
                    try {
                        val service = gatt.getService(NearbyBleProtocol.NEARBY_SERVICE_UUID)
                        val requestChar = service?.getCharacteristic(NearbyBleProtocol.CONNECTION_REQUEST_CHAR_UUID)
                        if (requestChar != null) {
                            sendConnectionRequest(gatt, requestChar)
                        }
                    } catch (_: SecurityException) {}
                }
            }
        }
        
        @Suppress("DEPRECATION")
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic
        ) {
            super.onCharacteristicChanged(gatt, characteristic)
            // On Android 13+ (API 33), the 3-arg overload below is called instead. Avoid duplicate processing.
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                handleCharacteristicChange(gatt, characteristic, characteristic.value)
            }
        }
        
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray
        ) {
            super.onCharacteristicChanged(gatt, characteristic, value)
            handleCharacteristicChange(gatt, characteristic, value)
        }
        
        private fun handleCharacteristicChange(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, value: ByteArray?) {
            if (value == null || value.isEmpty()) {
                Log.w("NearbyGattClient", "NEARBY_BLE: Empty notification bytes received for ${characteristic.uuid}")
                return
            }

            if (characteristic.uuid == NearbyBleProtocol.CONNECTION_STATUS_CHAR_UUID) {
                if (value[0] == 1.toByte()) {
                    Log.d("NearbyGattClient", "NEARBY_BLE: Connection accepted by remote device ${gatt.device.address}.")
                    isConnected = true
                    connectedDeviceAddress = gatt.device.address
                    onConnectionStateChanged?.invoke(gatt.device.address, NearbyConnectionState.CONNECTED)
                } else if (value[0] == 2.toByte()) {
                    Log.d("NearbyGattClient", "NEARBY_BLE: Connection declined by remote device ${gatt.device.address}.")
                    isConnected = false
                    connectedDeviceAddress = null
                    onConnectionStateChanged?.invoke(gatt.device.address, NearbyConnectionState.DISCONNECTED)
                    disconnect()
                }
            } else if (characteristic.uuid == NearbyBleProtocol.NEARBY_PAYLOAD_CHAR_UUID) {
                val payloadString = String(value, Charsets.UTF_8)
                if (payloadString.isNotBlank()) {
                    Log.d("NearbyGattClient", "NEARBY_BLE: Received payload notification from ${gatt.device.address}, byte length=${value.size}")
                    onIncomingPayloadReceived?.invoke(gatt.device.address, payloadString)
                }
            }
        }
    }

    private fun sendConnectionRequest(gatt: BluetoothGatt, requestChar: BluetoothGattCharacteristic) {
        try {
            val prefs = context.getSharedPreferences("smart_sos_settings", Context.MODE_PRIVATE)
            val myName = prefs.getString("nearby_device_name", NearbyBleProtocol.DEFAULT_DEVICE_NAME)?.trim()
            val nameBytes = (if (myName.isNullOrBlank()) NearbyBleProtocol.DEFAULT_DEVICE_NAME else myName)
                .toByteArray(Charsets.UTF_8).let { if (it.size > 24) it.copyOfRange(0, 24) else it }
            val payload = byteArrayOf(1) + nameBytes
            requestChar.value = payload
            gatt.writeCharacteristic(requestChar)
            Log.d("NearbyGattClient", "NEARBY_BLE: Sent connection request with name payload.")
        } catch (_: SecurityException) {}
    }

    fun connectToDevice(macAddress: String) {
        if (bluetoothGatt != null) {
            disconnect()
        }
        isConnected = false
        connectedDeviceAddress = null
        try {
            val device = bluetoothAdapter?.getRemoteDevice(macAddress)
            if (device != null) {
                bluetoothGatt = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    device.connectGatt(context, false, gattCallback, BluetoothDevice.TRANSPORT_LE)
                } else {
                    device.connectGatt(context, false, gattCallback)
                }
                onConnectionStateChanged?.invoke(macAddress, NearbyConnectionState.REQUESTING)
                Log.d("NearbyGattClient", "NEARBY_BLE: Initiated GATT connection to $macAddress (TRANSPORT_LE)")
            }
        } catch (e: SecurityException) {
            Log.e("NearbyGattClient", "Missing BLUETOOTH_CONNECT permission", e)
        }
    }

    fun sendPayload(payload: String): Boolean {
        val gatt = bluetoothGatt
        if (gatt == null) {
            Log.w("NearbyGattClient", "NEARBY_BLE: Cannot send payload: bluetoothGatt is null")
            return false
        }
        try {
            val payloadBytes = payload.toByteArray(Charsets.UTF_8)
            if (payloadBytes.size > NearbyBleProtocol.MAX_PAYLOAD_SIZE) {
                Log.e("NearbyGattClient", "NEARBY_BLE: Payload too large: ${payloadBytes.size} bytes (max ${NearbyBleProtocol.MAX_PAYLOAD_SIZE})")
                return false
            }
            
            val service = gatt.getService(NearbyBleProtocol.NEARBY_SERVICE_UUID)
            val payloadChar = service?.getCharacteristic(NearbyBleProtocol.NEARBY_PAYLOAD_CHAR_UUID)
            if (payloadChar != null) {
                payloadChar.value = payloadBytes
                val writeSuccess = gatt.writeCharacteristic(payloadChar)
                Log.d("NearbyGattClient", "NEARBY_BLE: writeCharacteristic invoked for ${gatt.device.address}, success=$writeSuccess")
                return writeSuccess
            } else {
                Log.e("NearbyGattClient", "NEARBY_BLE: payload characteristic not found on client connection")
            }
        } catch (e: SecurityException) {
            Log.e("NearbyGattClient", "Security Exception on send payload", e)
        }
        return false
    }

    fun disconnect() {
        try {
            bluetoothGatt?.disconnect()
        } catch (_: SecurityException) {}
    }
    
    private fun closeGatt() {
        try {
            isConnected = false
            connectedDeviceAddress = null
            bluetoothGatt?.close()
            bluetoothGatt = null
        } catch (_: SecurityException) {}
    }
}
