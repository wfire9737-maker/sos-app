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
    
    // Track active connected remote BluetoothDevice
    private var connectedDevice: BluetoothDevice? = null
    // Track remote devices that have subscribed to payload notifications via CCCD
    private val subscribedPayloadDevices = java.util.concurrent.ConcurrentHashMap.newKeySet<String>()

    var onRemoteDeviceDisconnected: ((String) -> Unit)? = null
    var onConnectionRequested: ((String, String?) -> Unit)? = null
    var onIncomingPayloadReceived: ((String, String) -> Unit)? = null

    var activeConnections = 0
        private set

    fun hasActiveConnections(): Boolean = activeConnections > 0

    var onActiveConnectionsChanged: ((Int) -> Unit)? = null

    var currentMtu: Int = 23
        private set

    private val gattServerCallback = object : BluetoothGattServerCallback() {
        override fun onConnectionStateChange(device: BluetoothDevice, status: Int, newState: Int) {
            super.onConnectionStateChange(device, status, newState)
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.d("NearbyGattServer", "NEARBY_BLE: Device connected to GATT server: ${device.address}")
                connectedDevice = device
                activeConnections++
                onActiveConnectionsChanged?.invoke(activeConnections)
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.d("NearbyGattServer", "NEARBY_BLE: Device disconnected from GATT server: ${device.address}")
                subscribedPayloadDevices.remove(device.address)
                if (activeConnections > 0) activeConnections--
                onActiveConnectionsChanged?.invoke(activeConnections)
                if (connectedDevice?.address == device.address) {
                    connectedDevice = null
                    onRemoteDeviceDisconnected?.invoke(device.address)
                }
            }
        }

        override fun onNotificationSent(device: BluetoothDevice, status: Int) {
            super.onNotificationSent(device, status)
            Log.d("NearbyGattServer", "NEARBY_BLE: onNotificationSent device=${device.address} status=$status (${if (status == BluetoothGatt.GATT_SUCCESS) "SUCCESS" else "FAILURE"})")
        }

        override fun onMtuChanged(device: BluetoothDevice, mtu: Int) {
            super.onMtuChanged(device, mtu)
            currentMtu = mtu
            Log.d("NearbyGattServer", "NEARBY_BLE: Server MTU changed for ${device.address} to $mtu")
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
                        } catch (_: Exception) {}
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
                    connectedDevice = device
                    if (value != null) {
                        val payloadString = String(value, Charsets.UTF_8)
                        Log.d("NearbyGattServer", "NEARBY_BLE: Received write payload from ${device.address}, byte length=${value.size}")
                        onIncomingPayloadReceived?.invoke(device.address, payloadString)
                    }
                } catch (e: SecurityException) {
                    Log.e("NearbyGattServer", "Security Exception on payload write", e)
                }
            } else {
                try {
                    if (responseNeeded) {
                        gattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_FAILURE, offset, null)
                    }
                } catch (_: SecurityException) {}
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
            
            val isCccd = descriptor.uuid == UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
            if (isCccd && value != null) {
                val isValid = value.contentEquals(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE) ||
                        value.contentEquals(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE) ||
                        value.contentEquals(BluetoothGattDescriptor.ENABLE_INDICATION_VALUE)
                if (isValid) {
                    descriptor.value = value
                    if (descriptor.characteristic?.uuid == NearbyBleProtocol.NEARBY_PAYLOAD_CHAR_UUID) {
                        if (value.contentEquals(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE) ||
                            value.contentEquals(BluetoothGattDescriptor.ENABLE_INDICATION_VALUE)
                        ) {
                            subscribedPayloadDevices.add(device.address)
                            Log.d("NearbyGattServer", "NEARBY_BLE: Payload notifications subscribed by ${device.address}")
                        } else {
                            subscribedPayloadDevices.remove(device.address)
                            Log.d("NearbyGattServer", "NEARBY_BLE: Payload notifications unsubscribed by ${device.address}")
                        }
                    }
                    Log.d("NearbyGattServer", "NEARBY_BLE: CCCD written for ${descriptor.characteristic.uuid} by ${device.address}: value=${value.contentToString()}")
                    if (responseNeeded) {
                        try {
                            gattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, value)
                        } catch (e: SecurityException) {
                            Log.e("NearbyGattServer", "SecurityException sending CCCD response", e)
                        }
                    }
                    return
                } else {
                    Log.w("NearbyGattServer", "NEARBY_BLE: Invalid CCCD value rejected: ${value.contentToString()}")
                    if (responseNeeded) {
                        try {
                            gattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_REQUEST_NOT_SUPPORTED, offset, null)
                        } catch (_: SecurityException) {}
                    }
                    return
                }
            }

            if (responseNeeded) {
                try {
                    descriptor.value = value
                    gattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, value)
                } catch (_: SecurityException) {}
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
            Log.d("NearbyGattServer", "NEARBY_BLE: Nearby GATT Server started")
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
            subscribedPayloadDevices.clear()
        } catch (_: SecurityException) {}
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
        
        val device = connectedDevice
        if (device != null && (macAddress.isBlank() || device.address.equals(macAddress, ignoreCase = true))) {
            try {
                val service = gattServer?.getService(NearbyBleProtocol.NEARBY_SERVICE_UUID)
                val statusChar = service?.getCharacteristic(NearbyBleProtocol.CONNECTION_STATUS_CHAR_UUID)
                if (statusChar != null) {
                    val statusBytes = byteArrayOf(1) // 1 = Accepted
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        gattServer?.notifyCharacteristicChanged(device, statusChar, false, statusBytes)
                    } else {
                        statusChar.value = statusBytes
                        gattServer?.notifyCharacteristicChanged(device, statusChar, false)
                    }
                    Log.d("NearbyGattServer", "NEARBY_BLE: Accepted connection for ${device.address}")
                }
            } catch (e: SecurityException) {
                Log.e("NearbyGattServer", "SecurityException accepting connection", e)
            }
        }
    }
    
    fun declineConnection(macAddress: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(macAddress.hashCode())
        
        val device = connectedDevice
        if (device != null && (macAddress.isBlank() || device.address.equals(macAddress, ignoreCase = true))) {
            try {
                Log.d("NearbyGattServer", "NEARBY_BLE: Declined connection for ${device.address}")
                val service = gattServer?.getService(NearbyBleProtocol.NEARBY_SERVICE_UUID)
                val statusChar = service?.getCharacteristic(NearbyBleProtocol.CONNECTION_STATUS_CHAR_UUID)
                if (statusChar != null) {
                    val statusBytes = byteArrayOf(2) // 2 = Declined
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        gattServer?.notifyCharacteristicChanged(device, statusChar, false, statusBytes)
                    } else {
                        statusChar.value = statusBytes
                        gattServer?.notifyCharacteristicChanged(device, statusChar, false)
                    }
                }
                gattServer?.cancelConnection(device)
            } catch (_: SecurityException) {}
        }
    }

    fun disconnectDevice(macAddress: String) {
        val device = connectedDevice
        if (device != null && (macAddress.isBlank() || device.address.equals(macAddress, ignoreCase = true))) {
            try {
                gattServer?.cancelConnection(device)
            } catch (_: SecurityException) {}
        }
    }

    fun sendPayloadNotification(targetAddress: String, payload: String): Boolean {
        val targetDevice = connectedDevice
        if (targetDevice == null) {
            Log.w("NearbyGattServer", "NEARBY_BLE: Cannot send payload notification: no connected device on server.")
            return false
        }
        
        if (targetAddress.isNotBlank() && !targetDevice.address.equals(targetAddress, ignoreCase = true)) {
            Log.w("NearbyGattServer", "NEARBY_BLE: Target address $targetAddress does not match connected device ${targetDevice.address}")
            return false
        }

        if (!subscribedPayloadDevices.contains(targetDevice.address)) {
            Log.w("NearbyGattServer", "NEARBY_BLE: Target device ${targetDevice.address} has not enabled payload notifications via CCCD. Notification aborted.")
            return false
        }

        try {
            val payloadBytes = payload.toByteArray(Charsets.UTF_8)
            if (payloadBytes.size > NearbyBleProtocol.MAX_PAYLOAD_SIZE) {
                Log.e("NearbyGattServer", "NEARBY_BLE: Payload too large: ${payloadBytes.size} bytes (max ${NearbyBleProtocol.MAX_PAYLOAD_SIZE})")
                return false
            }
            
            val service = gattServer?.getService(NearbyBleProtocol.NEARBY_SERVICE_UUID)
            val payloadChar = service?.getCharacteristic(NearbyBleProtocol.NEARBY_PAYLOAD_CHAR_UUID)
            if (payloadChar == null) {
                Log.e("NearbyGattServer", "NEARBY_BLE: NEARBY_PAYLOAD_CHAR not found on GATT server")
                return false
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val statusCode = gattServer?.notifyCharacteristicChanged(targetDevice, payloadChar, false, payloadBytes)
                val success = (statusCode == BluetoothStatusCodes.SUCCESS)
                Log.d(
                    "NearbyGattServer",
                    "NEARBY_BLE: notifyCharacteristicChanged (API 33+) target=${targetDevice.address} length=${payloadBytes.size} statusCode=$statusCode success=$success"
                )
                return success
            } else {
                payloadChar.value = payloadBytes
                val success = gattServer?.notifyCharacteristicChanged(targetDevice, payloadChar, false) ?: false
                Log.d(
                    "NearbyGattServer",
                    "NEARBY_BLE: notifyCharacteristicChanged (Legacy) target=${targetDevice.address} length=${payloadBytes.size} success=$success"
                )
                return success
            }
        } catch (e: SecurityException) {
            Log.e("NearbyGattServer", "Security Exception on send payload notification", e)
        } catch (e: Exception) {
            Log.e("NearbyGattServer", "Error sending payload notification", e)
        }
        return false
    }
}
