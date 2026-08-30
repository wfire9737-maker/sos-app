import re

with open("app/src/main/java/com/example/ui/GuardianViewModel.kt", "r") as f:
    content = f.read()

target = """    val historyService: HistoryService,
    val historyProvider: HistoryProvider,"""
replacement = """    val historyService: HistoryService,
    val historyProvider: HistoryProvider,
    val nearbyBleManager: com.example.ble.nearby.NearbyBleManager,"""

content = content.replace(target, replacement)

with open("app/src/main/java/com/example/ui/GuardianViewModel.kt", "w") as f:
    f.write(content)
