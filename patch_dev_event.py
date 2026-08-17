import re

file_path = "app/src/main/java/com/example/service/DeviceService.kt"

with open(file_path, "r") as f:
    content = f.read()

# Add shared flow
flow_var = """
    private val _incomingEsp32SosEvent = kotlinx.coroutines.flow.MutableSharedFlow<String>(extraBufferCapacity = 1)
    val incomingEsp32SosEvent: kotlinx.coroutines.flow.SharedFlow<String> = _incomingEsp32SosEvent.asSharedFlow()
"""

content = content.replace("    private var esp32PollingJob: Job? = null", "    private var esp32PollingJob: Job? = null\n" + flow_var)

# Trigger flow
trigger_logic = """
                                        addCommLog("🚨 ESP32 Hardware SOS Button Activated!")
                                        _incomingEsp32SosEvent.tryEmit("ESP32_BUTTON")
"""

content = content.replace('                                        addCommLog("🚨 ESP32 Hardware SOS Button Activated!")', trigger_logic.strip("\n"))

with open(file_path, "w") as f:
    f.write(content)

print("Patch applied to DeviceService.kt for event")
