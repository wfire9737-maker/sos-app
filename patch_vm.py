import re

file_path = "app/src/main/java/com/example/ui/GuardianViewModel.kt"

with open(file_path, "r") as f:
    content = f.read()

collect_logic = """
        viewModelScope.launch {
            deviceService.incomingEsp32SosEvent.collect { triggerType ->
                triggerEsp32SOS(triggerType)
            }
        }

        viewModelScope.launch {
"""

content = content.replace("        viewModelScope.launch {\n            emergencyProvider.activeEmergencyState", collect_logic.strip("\n") + "\n            emergencyProvider.activeEmergencyState")

with open(file_path, "w") as f:
    f.write(content)

print("Patch applied to GuardianViewModel.kt")
