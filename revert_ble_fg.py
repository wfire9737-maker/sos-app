import re

with open("app/src/main/java/com/example/service/BleForegroundService.kt", "r") as f:
    content = f.read()

# Revert imports
content = content.replace("import com.example.ble.nearby.NearbyBleManager\n", "")
content = content.replace("import com.example.repository.SettingsRepository\n", "")

# Revert injections
original_injection = """
    @Inject
    lateinit var bleManager: BleManager
"""
current_injections = """
    @Inject
    lateinit var bleManager: BleManager

    @Inject
    lateinit var nearbyBleManager: NearbyBleManager

    @Inject
    lateinit var settingsRepository: SettingsRepository
"""
content = content.replace(current_injections, original_injection)

# Revert nearby observation
observation = """        bleManager.connectionState.onEach { state ->
            updateNotification(state)
        }.launchIn(serviceScope)

        settingsRepository.nearbyPresenceInterval.onEach { interval ->
            nearbyBleManager.updatePresenceSettings(interval)
        }.launchIn(serviceScope)"""
original_observation = """        bleManager.connectionState.onEach { state ->
            updateNotification(state)
        }.launchIn(serviceScope)"""
content = content.replace(observation, original_observation)

# Revert cleanup
cleanup = """    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel()
        bleManager.disconnect()
        nearbyBleManager.updatePresenceSettings(0)
        Log.d("BleService", "BleForegroundService destroyed")
    }"""
original_cleanup = """    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel()
        bleManager.disconnect()
        Log.d("BleService", "BleForegroundService destroyed")
    }"""
content = content.replace(cleanup, original_cleanup)

with open("app/src/main/java/com/example/service/BleForegroundService.kt", "w") as f:
    f.write(content)

