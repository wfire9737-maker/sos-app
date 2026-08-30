import re

with open("app/src/main/java/com/example/ble/BleManager.kt", "r") as f:
    content = f.read()

old_debounce = """            val rawEventId = if (text.contains(":")) {
                text.substringAfter(":").trim()
            } else {
                "${internalEventCounter.get() + 1}"
            }
            val now = System.currentTimeMillis()
            // Prevent processing identical eventId if duplicate packet fired within 500ms
            if (lastProcessedEventId == rawEventId && (now - lastProcessedTimestamp) < 500) {
                Log.d("BleManager", "BLE: duplicate packet for event $rawEventId ignored")
                return
            }
            lastProcessedEventId = rawEventId
            lastProcessedTimestamp = now"""

new_debounce = """            val now = System.currentTimeMillis()
            // Prevent processing duplicate packet if fired within 800ms
            if ((now - lastProcessedTimestamp) < 800) {
                Log.d("BleManager", "BLE: duplicate SOS packet ignored")
                return
            }
            lastProcessedTimestamp = now
            
            val rawEventId = if (text.contains(":")) {
                text.substringAfter(":").trim()
            } else {
                "${internalEventCounter.get() + 1}"
            }
            lastProcessedEventId = rawEventId"""

content = content.replace(old_debounce, new_debounce)

with open("app/src/main/java/com/example/ble/BleManager.kt", "w") as f:
    f.write(content)

