import os

filepath = "app/src/main/java/com/example/service/DeviceService.kt"
with open(filepath, "r") as f:
    content = f.read()

target = """    private fun startConnectivityMonitors() {
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

replacement = """    private fun startConnectivityMonitors() {
        try {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            connectivityManager.registerDefaultNetworkCallback(object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    setNetworkAvailable(true)
                    refreshDeviceStatus()
                }
                override fun onLost(network: Network) {
                    setNetworkAvailable(false)
                    refreshDeviceStatus()
                }
            })
            
            val bluetoothReceiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent?) {
                    if (intent?.action == BluetoothAdapter.ACTION_STATE_CHANGED) {
                        val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
                        if (state == BluetoothAdapter.STATE_OFF || state == BluetoothAdapter.STATE_TURNING_OFF) {
                            addCommLog("⚠️ Bluetooth was disabled on phone. Connection to ESP32 lost.")
                            refreshDeviceStatus()
                        } else if (state == BluetoothAdapter.STATE_ON) {
                            addCommLog("✅ Bluetooth enabled. Ready to connect.")
                            refreshDeviceStatus()
                        }
                    }
                }
            }
            context.registerReceiver(bluetoothReceiver, IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED))
        } catch (e: Exception) {
            Log.e("DeviceService", "Error starting connectivity monitors", e)
        }
    }"""

content = content.replace(target, replacement, 1)

with open(filepath, "w") as f:
    f.write(content)
