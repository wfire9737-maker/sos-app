import re

with open("app/src/main/java/com/example/service/NearbyBleService.kt", "r") as f:
    content = f.read()

target = """    override fun onCreate() {
        super.onCreate()
        Log.d("NearbyBleService", "NEARBY_SERVICE: started")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("NearbyBleService", "NEARBY_SERVICE: onStartCommand")
        
        prefs.registerOnSharedPreferenceChangeListener(prefListener)
        val initialInterval = prefs.getInt("nearby_presence_interval", 0)
        nearbyBleManager.updatePresenceSettings(initialInterval)

        return START_STICKY
    }"""

replacement = """    override fun onCreate() {
        super.onCreate()
        prefs.registerOnSharedPreferenceChangeListener(prefListener)
        Log.d("NearbyBleService", "NEARBY_SERVICE: started")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("NearbyBleService", "NEARBY_SERVICE: onStartCommand")
        
        val initialInterval = prefs.getInt("nearby_presence_interval", 0)
        nearbyBleManager.updatePresenceSettings(initialInterval)

        return START_STICKY
    }"""

content = content.replace(target, replacement)

with open("app/src/main/java/com/example/service/NearbyBleService.kt", "w") as f:
    f.write(content)
