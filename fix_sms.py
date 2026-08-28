import re

with open("app/src/main/java/com/example/service/EmergencyService.kt", "r") as f:
    content = f.read()

# Change SMS format if lat/lng are 0.0
new_sms = """
            val locationStr = if (model.latitude != 0.0 && model.longitude != 0.0) {
                "Location:\\nhttps://maps.google.com/?q=${model.latitude},${model.longitude}"
            } else {
                "Location: UNAVAILABLE"
            }
            val message = if (isUpdate) {
                "LIVE UPDATE!\\n${model.userName} is still in an active emergency.\\n\\n$locationStr\\n\\nTime: $timestamp"
            } else {
                "EMERGENCY!\\n${model.userName} has triggered an SOS.\\n\\n$locationStr\\n\\nPlease contact immediately.\\n\\nTime: $timestamp"
            }
"""

content = re.sub(
    r'val message = if \(isUpdate\) \{[\s\S]*?\}',
    new_sms.strip(),
    content
)

with open("app/src/main/java/com/example/service/EmergencyService.kt", "w") as f:
    f.write(content)
