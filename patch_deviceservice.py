with open("app/src/main/java/com/example/service/DeviceService.kt", "r") as f:
    content = f.read()

import_block = """
    private var networkCallback: ConnectivityManager.NetworkCallback? = null
    private var bluetoothReceiver: BroadcastReceiver? = null
"""

content = content.replace("private var telemetryJob: Job? = null", "private var telemetryJob: Job? = null\n" + import_block)

network_callback_old = """            connectivityManager.registerDefaultNetworkCallback(object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    setNetworkAvailable(true)
                    refreshDeviceStatus()
                }
                override fun onLost(network: Network) {
                    setNetworkAvailable(false)
                    refreshDeviceStatus()
                }
            })"""

network_callback_new = """            networkCallback = object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    setNetworkAvailable(true)
                    refreshDeviceStatus()
                }
                override fun onLost(network: Network) {
                    setNetworkAvailable(false)
                    refreshDeviceStatus()
                }
            }
            connectivityManager.registerDefaultNetworkCallback(networkCallback!!)"""

content = content.replace(network_callback_old, network_callback_new)

bt_receiver_old = """            val bluetoothReceiver = object : BroadcastReceiver() {"""
bt_receiver_new = """            bluetoothReceiver = object : BroadcastReceiver() {"""

content = content.replace(bt_receiver_old, bt_receiver_new)

cleanup_method = """
    fun cleanup() {
        telemetryJob?.cancel()
        try {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            networkCallback?.let { connectivityManager.unregisterNetworkCallback(it) }
            bluetoothReceiver?.let { context.unregisterReceiver(it) }
        } catch (e: Exception) {
            Log.e("DeviceService", "Error during cleanup", e)
        }
    }
"""

content = content.replace("    private fun startConnectivityMonitors() {", cleanup_method + "\n    private fun startConnectivityMonitors() {")

with open("app/src/main/java/com/example/service/DeviceService.kt", "w") as f:
    f.write(content)
