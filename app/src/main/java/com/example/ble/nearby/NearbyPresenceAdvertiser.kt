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

class NearbyPresenceAdvertiser(private val context: Context) {
    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager?.adapter
    private val advertiser: BluetoothLeAdvertiser? = bluetoothAdapter?.bluetoothLeAdvertiser
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

    fun startAdvertising() {
        if (isAdvertising) return
        try {
            if (advertiser == null) {
                Log.w("NearbyAdvertiser", "Bluetooth LE Advertiser not available.")
                return
            }

            val prefs = context.getSharedPreferences("smart_sos_settings", Context.MODE_PRIVATE)
            val configuredName = prefs.getString("nearby_device_name", NearbyBleProtocol.DEFAULT_DEVICE_NAME)?.trim()
            val deviceName = if (configuredName.isNullOrBlank()) NearbyBleProtocol.DEFAULT_DEVICE_NAME else configuredName
            val nameBytes = deviceName.toByteArray(Charsets.UTF_8).let {
                if (it.size > 24) it.copyOfRange(0, 24) else it
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
                .addServiceData(ParcelUuid(NearbyBleProtocol.NEARBY_NAME_SERVICE_UUID), nameBytes)
                .build()

            advertiser.startAdvertising(settings, data, scanResponse, advertiseCallback)
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
