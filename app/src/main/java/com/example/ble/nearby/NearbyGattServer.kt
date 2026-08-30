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

    private val gattServerCallback = object : BluetoothGattServerCallback() {
        override fun onConnectionStateChange(device: BluetoothDevice, status: Int, newState: Int) {
            super.onConnectionStateChange(device, status, newState)
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.d("NearbyGattServer", "Device connected: ${device.address}")
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.d("NearbyGattServer", "Device disconnected: ${device.address}")
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
                    showConnectionRequestNotification(device.address)
                } catch (e: SecurityException) {
                    Log.e("NearbyGattServer", "Security Exception on write request", e)
                }
            } else {
                try {
                    if (responseNeeded) {
                        gattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_FAILURE, offset, null)
                    }
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
            
            val configDesc = BluetoothGattDescriptor(
                UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"),
                BluetoothGattDescriptor.PERMISSION_READ or BluetoothGattDescriptor.PERMISSION_WRITE
            )
            statusChar.addDescriptor(configDesc)
            
            service.addCharacteristic(requestChar)
            service.addCharacteristic(statusChar)
            
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
    
    private fun showConnectionRequestNotification(macAddress: String) {
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
        
        val safeId = macAddress.takeLast(4)
        val notification = NotificationCompat.Builder(context, "nearby_requests")
            .setSmallIcon(android.R.drawable.stat_sys_data_bluetooth)
            .setContentTitle("Nearby connection request")
            .setContentText("User ($safeId) wants to connect.")
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
                gattServer?.cancelConnection(connectedDevice)
            } catch (e: SecurityException) {}
        }
    }
}
