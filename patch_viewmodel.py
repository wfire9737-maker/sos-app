import re

with open("app/src/main/java/com/example/ui/GuardianViewModel.kt", "r") as f:
    content = f.read()

target = """    val voiceSosService: VoiceSosService,
    val historyService: HistoryService,
    private val app: Application"""
replacement = """    val voiceSosService: VoiceSosService,
    val historyService: HistoryService,
    val nearbyBleManager: com.example.ble.nearby.NearbyBleManager,
    private val app: Application"""

content = content.replace(target, replacement)

with open("app/src/main/java/com/example/ui/GuardianViewModel.kt", "w") as f:
    f.write(content)
