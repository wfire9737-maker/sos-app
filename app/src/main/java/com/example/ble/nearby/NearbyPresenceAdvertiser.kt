package com.example.ble.nearby

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.BluetoothLeAdvertiser
import android.content.Context
import android.os.ParcelUuid
import android.util.Log
import java.util.UUID

class NearbyPresenceAdvertiser(private val context: Context) {
    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter?
        get() = bluetoothManager?.adapter
    private val advertiser: BluetoothLeAdvertiser?
        get() = bluetoothAdapter?.bluetoothLeAdvertiser
    private var isAdvertising = false

    private val advertiseCallback = object : AdvertiseCallback() {
        override fun onStartSuccess(settingsInEffect: AdvertiseSettings?) {
            super.onStartSuccess(settingsInEffect)
            isAdvertising = true
            Log.d("NearbyAdvertiser", "Successfully started advertising nearby presence.")
        }

        override fun onStartFailure(errorCode: Int) {
            super.onStartFailure(errorCode)
            isAdvertising = false
            Log.e("NearbyAdvertiser", "Failed to start advertising nearby presence. Error code: $errorCode")
        }
    }

    private fun getOrCreateStableDeviceId(): String {
        val prefs = context.getSharedPreferences("smart_sos_settings", Context.MODE_PRIVATE)
        var deviceId = prefs.getString(NearbyBleProtocol.PREFS_KEY_STABLE_DEVICE_ID, null)
        if (deviceId.isNullOrBlank()) {
            // Generate a compact 12-char hex unique device identifier (48 bits entropy)
            deviceId = UUID.randomUUID().toString().replace("-", "").take(12)
            prefs.edit().putString(NearbyBleProtocol.PREFS_KEY_STABLE_DEVICE_ID, deviceId).apply()
        }
        return deviceId
    }

    fun startAdvertising() {
        if (isAdvertising) return
        try {
            val leAdvertiser = advertiser
            if (leAdvertiser == null) {
                Log.w("NearbyAdvertiser", "Bluetooth LE Advertiser not available.")
                return
            }

            val stableDeviceId = getOrCreateStableDeviceId()
            val prefs = context.getSharedPreferences("smart_sos_settings", Context.MODE_PRIVATE)
            val configuredName = prefs.getString("nearby_device_name", NearbyBleProtocol.DEFAULT_DEVICE_NAME)?.trim()
            val deviceName = if (configuredName.isNullOrBlank()) NearbyBleProtocol.DEFAULT_DEVICE_NAME else configuredName

            // Format: "${stableDeviceId}:${deviceName}" (e.g. "a1b2c3d4e5f6:Redmi Note 13")
            val payloadString = "$stableDeviceId:$deviceName"
            val payloadBytes = payloadString.toByteArray(Charsets.UTF_8).let {
                if (it.size > 26) it.copyOfRange(0, 26) else it
            }

            val settings = AdvertiseSettings.Builder()
                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
                .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM)
                .setConnectable(true)
                .build()

            val data = AdvertiseData.Builder()
                .setIncludeDeviceName(false)
                .addServiceUuid(ParcelUuid(NearbyBleProtocol.NEARBY_SERVICE_UUID))
                .build()

            val scanResponse = AdvertiseData.Builder()
                .setIncludeDeviceName(false)
                .addServiceData(ParcelUuid(NearbyBleProtocol.NEARBY_NAME_SERVICE_UUID), payloadBytes)
                .build()

            leAdvertiser.startAdvertising(settings, data, scanResponse, advertiseCallback)
        } catch (e: SecurityException) {
            Log.e("NearbyAdvertiser", "Missing BLUETOOTH_ADVERTISE permission", e)
        }
    }

    fun stopAdvertising() {
        if (!isAdvertising) return
        try {
            advertiser?.stopAdvertising(advertiseCallback)
            isAdvertising = false
            Log.d("NearbyAdvertiser", "Stopped advertising nearby presence.")
        } catch (e: SecurityException) {
            Log.e("NearbyAdvertiser", "Missing BLUETOOTH_ADVERTISE permission", e)
        }
    }
}
