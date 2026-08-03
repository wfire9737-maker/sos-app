import os

filepath = "app/src/main/java/com/example/service/DeviceService.kt"
with open(filepath, "r") as f:
    content = f.read()

import_target = "import android.util.Log"
import_replacement = """import android.util.Log
import android.content.Intent
import android.content.IntentFilter
import android.content.BroadcastReceiver
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.bluetooth.BluetoothAdapter"""

content = content.replace(import_target, import_replacement, 1)

init_target = """    init {
        startTelemetryLoop()
    }"""

init_replacement = """    init {
        startTelemetryLoop()
        startConnectivityMonitors()
    }

    private fun startConnectivityMonitors() {
        try {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            connectivityManager.registerDefaultNetworkCallback(object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    setNetworkAvailable(true)
                }
                override fun onLost(network: Network) {
                    setNetworkAvailable(false)
                }
            })
            
            val bluetoothReceiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent?) {
                    if (intent?.action == BluetoothAdapter.ACTION_STATE_CHANGED) {
                        val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
                        if (state == BluetoothAdapter.STATE_OFF || state == BluetoothAdapter.STATE_TURNING_OFF) {
                            addCommLog("⚠️ Bluetooth was disabled on phone. Connection to ESP32 lost.")
                            // Optionally handle device disconnect here
                        } else if (state == BluetoothAdapter.STATE_ON) {
                            addCommLog("✅ Bluetooth enabled. Ready to connect.")
                        }
                    }
                }
            }
            context.registerReceiver(bluetoothReceiver, IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED))
        } catch (e: Exception) {
            Log.e("DeviceService", "Error starting connectivity monitors", e)
        }
    }"""

content = content.replace(init_target, init_replacement, 1)

with open(filepath, "w") as f:
    f.write(content)

print("Updated DeviceService with sync logic")
