import os

filepath = "app/src/main/java/com/example/ui/GuardianViewModel.kt"
with open(filepath, "r") as f:
    content = f.read()

target = """    private suspend fun initiateEmergencySequence(triggerSource: String, deviceId: String): com.example.model.EmergencyModel {
        val user = (authState.value as? AuthState.Success)?.user"""
replacement = """    private suspend fun initiateEmergencySequence(triggerSource: String, deviceId: String): com.example.model.EmergencyModel {
        checkSystemReadiness()
        val user = (authState.value as? AuthState.Success)?.user"""

if target in content:
    content = content.replace(target, replacement)
    with open(filepath, "w") as f:
        f.write(content)
    print("Called checkSystemReadiness in initiateEmergencySequence")
else:
    print("Target not found in ViewModel")
