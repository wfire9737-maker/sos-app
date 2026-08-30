import re

with open("app/src/main/java/com/example/service/NearbyBleService.kt", "r") as f:
    content = f.read()

target = """    companion object {
        const val CHANNEL_ID = "nearby_presence_channel"
        const val NOTIFICATION_ID = 2002"""
        
replacement = """    companion object {
        const val CHANNEL_ID = "nearby_presence_channel"
        const val NOTIFICATION_ID = 2002
        
        const val ACTION_ACCEPT_CONNECTION = "com.example.service.NearbyBleService.ACTION_ACCEPT_CONNECTION"
        const val ACTION_DECLINE_CONNECTION = "com.example.service.NearbyBleService.ACTION_DECLINE_CONNECTION"
        const val EXTRA_MAC_ADDRESS = "extra_mac_address"
"""
content = content.replace(target, replacement)

target2 = """    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("NearbyBleService", "NEARBY_SERVICE: onStartCommand")
        
        val initialInterval = prefs.getInt("nearby_presence_interval", 0)"""
        
replacement2 = """    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("NearbyBleService", "NEARBY_SERVICE: onStartCommand")
        
        if (intent?.action == ACTION_ACCEPT_CONNECTION) {
            val mac = intent.getStringExtra(EXTRA_MAC_ADDRESS)
            if (mac != null) nearbyBleManager.acceptIncomingConnection(mac)
            return START_STICKY
        } else if (intent?.action == ACTION_DECLINE_CONNECTION) {
            val mac = intent.getStringExtra(EXTRA_MAC_ADDRESS)
            if (mac != null) nearbyBleManager.declineIncomingConnection(mac)
            return START_STICKY
        }
        
        val initialInterval = prefs.getInt("nearby_presence_interval", 0)"""
content = content.replace(target2, replacement2)

with open("app/src/main/java/com/example/service/NearbyBleService.kt", "w") as f:
    f.write(content)
