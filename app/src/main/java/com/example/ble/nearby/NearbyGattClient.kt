package com.example.ble.nearby

import android.bluetooth.*
import android.content.Context
import android.util.Log

class NearbyGattClient(private val context: Context) {
    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager?.adapter
    
    private var bluetoothGatt: BluetoothGatt? = null
    var onConnectionStateChanged: ((String, NearbyConnectionState) -> Unit)? = null

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            super.onConnectionStateChange(gatt, status, newState)
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.d("NearbyGattClient", "Connected to GATT server.")
                try {
                    gatt.discoverServices()
                } catch (e: SecurityException) {}
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.d("NearbyGattClient", "Disconnected from GATT server.")
                onConnectionStateChanged?.invoke(gatt.device.address, NearbyConnectionState.DISCONNECTED)
                closeGatt()
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            super.onServicesDiscovered(gatt, status)
            if (status == BluetoothGatt.GATT_SUCCESS) {
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
                            // If descriptor is missing, just write the request immediately
                            sendConnectionRequest(gatt, requestChar)
                        }
                    }
                } catch (e: SecurityException) {}
            }
        }

        override fun onDescriptorWrite(gatt: BluetoothGatt, descriptor: BluetoothGattDescriptor, status: Int) {
            super.onDescriptorWrite(gatt, descriptor, status)
            if (status == BluetoothGatt.GATT_SUCCESS && descriptor.characteristic.uuid == NearbyBleProtocol.CONNECTION_STATUS_CHAR_UUID) {
                try {
                    val service = gatt.getService(NearbyBleProtocol.NEARBY_SERVICE_UUID)
                    val requestChar = service?.getCharacteristic(NearbyBleProtocol.CONNECTION_REQUEST_CHAR_UUID)
                    if (requestChar != null) {
                        sendConnectionRequest(gatt, requestChar)
                    }
                } catch (e: SecurityException) {}
            }
        }
        
        @Suppress("DEPRECATION")
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic
        ) {
            super.onCharacteristicChanged(gatt, characteristic)
            handleCharacteristicChange(gatt, characteristic, characteristic.value)
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
            if (characteristic.uuid == NearbyBleProtocol.CONNECTION_STATUS_CHAR_UUID) {
                if (value != null && value.isNotEmpty() && value[0] == 1.toByte()) {
                    Log.d("NearbyGattClient", "Connection accepted by remote device.")
                    onConnectionStateChanged?.invoke(gatt.device.address, NearbyConnectionState.CONNECTED)
                }
            }
        }
    }

    private fun sendConnectionRequest(gatt: BluetoothGatt, requestChar: BluetoothGattCharacteristic) {
        try {
            requestChar.value = byteArrayOf(1)
            gatt.writeCharacteristic(requestChar)
            Log.d("NearbyGattClient", "Sent connection request.")
        } catch (e: SecurityException) {}
    }

    fun connectToDevice(macAddress: String) {
        // Prevent duplicate connections
        if (bluetoothGatt != null) {
            disconnect()
        }
        try {
            val device = bluetoothAdapter?.getRemoteDevice(macAddress)
            if (device != null) {
                bluetoothGatt = device.connectGatt(context, false, gattCallback)
                onConnectionStateChanged?.invoke(macAddress, NearbyConnectionState.REQUESTING)
                Log.d("NearbyGattClient", "Initiated GATT connection to $macAddress")
            }
        } catch (e: SecurityException) {
            Log.e("NearbyGattClient", "Missing BLUETOOTH_CONNECT permission", e)
        }
    }

    fun disconnect() {
        try {
            bluetoothGatt?.disconnect()
        } catch (e: SecurityException) {}
    }
    
    private fun closeGatt() {
        try {
            bluetoothGatt?.close()
            bluetoothGatt = null
        } catch (e: SecurityException) {}
    }
}
