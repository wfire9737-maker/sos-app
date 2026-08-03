import os

# Fix EmergencyService
filepath = "app/src/main/java/com/example/service/EmergencyService.kt"
with open(filepath, "r") as f:
    content = f.read()

target = """            val message = if (isUpdate) {
                "LIVE UPDATE!
${model.userName} is still in an active emergency.

Location:
https://maps.google.com/?q=${model.latitude},${model.longitude}

Time: $timestamp"
            } else {
                "EMERGENCY!
${model.userName} has triggered an SOS.

Location:
https://maps.google.com/?q=${model.latitude},${model.longitude}

Please contact immediately.

Time: $timestamp"
            }"""

replacement = '            val message = if (isUpdate) {\n                "LIVE UPDATE!\\n${model.userName} is still in an active emergency.\\n\\nLocation:\\nhttps://maps.google.com/?q=${model.latitude},${model.longitude}\\n\\nTime: $timestamp"\n            } else {\n                "EMERGENCY!\\n${model.userName} has triggered an SOS.\\n\\nLocation:\\nhttps://maps.google.com/?q=${model.latitude},${model.longitude}\\n\\nPlease contact immediately.\\n\\nTime: $timestamp"\n            }'

if target in content:
    content = content.replace(target, replacement)
    with open(filepath, "w") as f:
        f.write(content)
    print("Fixed EmergencyService SMS string")
else:
    print("Target not found in EmergencyService")


# Fix EmergencyScreen
filepath = "app/src/main/java/com/example/ui/screens/EmergencyScreen.kt"
with open(filepath, "r") as f:
    content = f.read()
    
target = """viewModel.cancelEmergencyWithPin("", "") { success -> """
replacement = """viewModel.cancelEmergencyWithPin("") { success -> """

if target in content:
    content = content.replace(target, replacement)
    with open(filepath, "w") as f:
        f.write(content)
    print("Fixed EmergencyScreen method call")
else:
    print("Target not found in EmergencyScreen")

