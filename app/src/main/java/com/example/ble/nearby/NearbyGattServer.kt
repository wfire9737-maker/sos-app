package com.example.ble.nearby

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.bluetooth.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.service.NearbyBleService
import java.util.UUID

class NearbyGattServer(private val context: Context) {
    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    private var gattServer: BluetoothGattServer? = null
    
    // Track connection internally
    private var connectedDevice: BluetoothDevice? = null
    var onRemoteDeviceDisconnected: ((String) -> Unit)? = null
    var onConnectionRequested: ((String, String?) -> Unit)? = null
    var onIncomingPayloadReceived: ((String, String) -> Unit)? = null

    var activeConnections = 0
        private set

    fun hasActiveConnections(): Boolean = activeConnections > 0

    var onActiveConnectionsChanged: ((Int) -> Unit)? = null

    private val gattServerCallback = object : BluetoothGattServerCallback() {
        override fun onConnectionStateChange(device: BluetoothDevice, status: Int, newState: Int) {
            super.onConnectionStateChange(device, status, newState)
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.d("NearbyGattServer", "Device connected: ${device.address}")
                activeConnections++
                onActiveConnectionsChanged?.invoke(activeConnections)
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.d("NearbyGattServer", "Device disconnected: ${device.address}")
                if (activeConnections > 0) activeConnections--
                onActiveConnectionsChanged?.invoke(activeConnections)
                if (connectedDevice?.address == device.address) {
                    connectedDevice = null
                    onRemoteDeviceDisconnected?.invoke(device.address)
                }
            }
        }

        override fun onCharacteristicWriteRequest(
            device: BluetoothDevice,
            requestId: Int,
            characteristic: BluetoothGattCharacteristic,
            preparedWrite: Boolean,
            responseNeeded: Boolean,
            offset: Int,
            value: ByteArray?
        ) {
            super.onCharacteristicWriteRequest(device, requestId, characteristic, preparedWrite, responseNeeded, offset, value)
            
            if (characteristic.uuid == NearbyBleProtocol.CONNECTION_REQUEST_CHAR_UUID) {
                try {
                    if (responseNeeded) {
                        gattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, value)
                    }
                    connectedDevice = device
                    var senderName: String? = null
                    if (value != null && value.size > 1) {
                        try {
                            val decoded = String(value.copyOfRange(1, value.size), Charsets.UTF_8).trim()
                            if (decoded.isNotBlank()) {
                                senderName = decoded
                            }
                        } catch (e: Exception) {}
                    }
                    onConnectionRequested?.invoke(device.address, senderName)
                    showConnectionRequestNotification(device.address, senderName)
                } catch (e: SecurityException) {
                    Log.e("NearbyGattServer", "Security Exception on write request", e)
                }
            } else if (characteristic.uuid == NearbyBleProtocol.NEARBY_PAYLOAD_CHAR_UUID) {
                try {
                    if (responseNeeded) {
                        gattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, value)
                    }
                    if (device.address == connectedDevice?.address && value != null) {
                        val payloadString = String(value, Charsets.UTF_8)
                        onIncomingPayloadReceived?.invoke(device.address, payloadString)
                    } else if (device.address != connectedDevice?.address) {
                        Log.w("NearbyGattServer", "Rejected payload from non-connected device: ${device.address}")
                    }
                } catch (e: SecurityException) {
                    Log.e("NearbyGattServer", "Security Exception on payload write", e)
                }
            } else {
                try {
                    if (responseNeeded) {
                        gattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_FAILURE, offset, null)
                    }
                } catch (e: SecurityException) {}
            }
        }

        override fun onDescriptorWriteRequest(
            device: BluetoothDevice,
            requestId: Int,
            descriptor: BluetoothGattDescriptor,
            preparedWrite: Boolean,
            responseNeeded: Boolean,
            offset: Int,
            value: ByteArray?
        ) {
            super.onDescriptorWriteRequest(device, requestId, descriptor, preparedWrite, responseNeeded, offset, value)
            if (responseNeeded) {
                try {
                    gattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, value)
                } catch (e: SecurityException) {}
            }
        }
    }

    fun startServer() {
        if (gattServer != null) return
        try {
            if (bluetoothManager == null) return
            gattServer = bluetoothManager.openGattServer(context, gattServerCallback)
            if (gattServer == null) return
            
            val service = BluetoothGattService(NearbyBleProtocol.NEARBY_SERVICE_UUID, BluetoothGattService.SERVICE_TYPE_PRIMARY)
            
            val requestChar = BluetoothGattCharacteristic(
                NearbyBleProtocol.CONNECTION_REQUEST_CHAR_UUID,
                BluetoothGattCharacteristic.PROPERTY_WRITE,
                BluetoothGattCharacteristic.PERMISSION_WRITE
            )
            
            val statusChar = BluetoothGattCharacteristic(
                NearbyBleProtocol.CONNECTION_STATUS_CHAR_UUID,
                BluetoothGattCharacteristic.PROPERTY_NOTIFY or BluetoothGattCharacteristic.PROPERTY_READ,
                BluetoothGattCharacteristic.PERMISSION_READ
            )
            
            val payloadChar = BluetoothGattCharacteristic(
                NearbyBleProtocol.NEARBY_PAYLOAD_CHAR_UUID,
                BluetoothGattCharacteristic.PROPERTY_WRITE or BluetoothGattCharacteristic.PROPERTY_NOTIFY,
                BluetoothGattCharacteristic.PERMISSION_WRITE
            )
            
            val configDesc = BluetoothGattDescriptor(
                UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"),
                BluetoothGattDescriptor.PERMISSION_READ or BluetoothGattDescriptor.PERMISSION_WRITE
            )
            statusChar.addDescriptor(configDesc)
            
            val payloadConfigDesc = BluetoothGattDescriptor(
                UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"),
                BluetoothGattDescriptor.PERMISSION_READ or BluetoothGattDescriptor.PERMISSION_WRITE
            )
            payloadChar.addDescriptor(payloadConfigDesc)
            
            service.addCharacteristic(requestChar)
            service.addCharacteristic(statusChar)
            service.addCharacteristic(payloadChar)
            
            gattServer?.addService(service)
            Log.d("NearbyGattServer", "Nearby GATT Server started")
        } catch (e: SecurityException) {
            Log.e("NearbyGattServer", "Missing permission to start GATT server", e)
        }
    }

    fun stopServer() {
        try {
            gattServer?.clearServices()
            gattServer?.close()
            gattServer = null
            connectedDevice = null
        } catch (e: SecurityException) {}
    }
    
    private fun showConnectionRequestNotification(macAddress: String, senderName: String? = null) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel("nearby_requests", "Nearby Connection Requests", NotificationManager.IMPORTANCE_HIGH)
            notificationManager.createNotificationChannel(channel)
        }
        
        val acceptIntent = Intent(context, NearbyBleService::class.java).apply {
            action = NearbyBleService.ACTION_ACCEPT_CONNECTION
            putExtra(NearbyBleService.EXTRA_MAC_ADDRESS, macAddress)
        }
        val acceptPending = PendingIntent.getService(context, macAddress.hashCode(), acceptIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        
        val declineIntent = Intent(context, NearbyBleService::class.java).apply {
            action = NearbyBleService.ACTION_DECLINE_CONNECTION
            putExtra(NearbyBleService.EXTRA_MAC_ADDRESS, macAddress)
        }
        val declinePending = PendingIntent.getService(context, macAddress.hashCode() + 1, declineIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        
        val displayName = if (!senderName.isNullOrBlank()) senderName else "User (${macAddress.takeLast(4)})"
        val notification = NotificationCompat.Builder(context, "nearby_requests")
            .setSmallIcon(android.R.drawable.stat_sys_data_bluetooth)
            .setContentTitle("Nearby connection request")
            .setContentText("$displayName wants to connect.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .addAction(0, "Accept", acceptPending)
            .addAction(0, "Decline", declinePending)
            .setAutoCancel(true)
            .build()
            
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                notificationManager.notify(macAddress.hashCode(), notification)
            }
        } else {
            notificationManager.notify(macAddress.hashCode(), notification)
        }
    }
    
    fun acceptConnection(macAddress: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(macAddress.hashCode())
        
        if (connectedDevice?.address == macAddress) {
            try {
                val service = gattServer?.getService(NearbyBleProtocol.NEARBY_SERVICE_UUID)
                val statusChar = service?.getCharacteristic(NearbyBleProtocol.CONNECTION_STATUS_CHAR_UUID)
                if (statusChar != null) {
                    statusChar.value = byteArrayOf(1) // 1 = Accepted
                    gattServer?.notifyCharacteristicChanged(connectedDevice, statusChar, false)
                    Log.d("NearbyGattServer", "Accepted connection for $macAddress")
                }
            } catch (e: SecurityException) {}
        }
    }
    
    fun declineConnection(macAddress: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(macAddress.hashCode())
        
        if (connectedDevice?.address == macAddress) {
            try {
                Log.d("NearbyGattServer", "Declined connection for $macAddress")
                val service = gattServer?.getService(NearbyBleProtocol.NEARBY_SERVICE_UUID)
                val statusChar = service?.getCharacteristic(NearbyBleProtocol.CONNECTION_STATUS_CHAR_UUID)
                if (statusChar != null) {
                    statusChar.value = byteArrayOf(2) // 2 = Declined
                    gattServer?.notifyCharacteristicChanged(connectedDevice, statusChar, false)
                }
                gattServer?.cancelConnection(connectedDevice)
            } catch (e: SecurityException) {}
        }
    }

    fun disconnectDevice(macAddress: String) {
        if (connectedDevice?.address == macAddress) {
            try {
                gattServer?.cancelConnection(connectedDevice)
            } catch (e: SecurityException) {}
        }
    }

    fun sendPayloadNotification(macAddress: String, payload: String): Boolean {
        if (connectedDevice?.address != macAddress) {
            Log.w("NearbyGattServer", "Cannot send payload: device not connected.")
            return false
        }
        try {
            val payloadBytes = payload.toByteArray(Charsets.UTF_8)
            if (payloadBytes.size > NearbyBleProtocol.MAX_PAYLOAD_SIZE) {
                Log.e("NearbyGattServer", "Payload too large: ${payloadBytes.size} bytes (max ${NearbyBleProtocol.MAX_PAYLOAD_SIZE})")
                return false
            }
            
            val service = gattServer?.getService(NearbyBleProtocol.NEARBY_SERVICE_UUID)
            val payloadChar = service?.getCharacteristic(NearbyBleProtocol.NEARBY_PAYLOAD_CHAR_UUID)
            if (payloadChar != null) {
                payloadChar.value = payloadBytes
                return gattServer?.notifyCharacteristicChanged(connectedDevice, payloadChar, false) ?: false
            }
        } catch (e: SecurityException) {
            Log.e("NearbyGattServer", "Security Exception on send payload notification", e)
        }
        return false
    }
}
