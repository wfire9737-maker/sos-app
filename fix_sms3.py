import re

with open("app/src/main/java/com/example/service/EmergencyService.kt", "r") as f:
    content = f.read()

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
    r'val locationStr = if \(model\.latitude != 0\.0[\s\S]*?Time: \$timestamp"',
    new_sms.strip(),
    content
)

content = re.sub(
    r'val locationStr = if \(model\.latitude != 0\.0.*?Time: \$timestamp"',
    new_sms.strip(),
    content,
    flags=re.DOTALL
)

with open("app/src/main/java/com/example/service/EmergencyService.kt", "w") as f:
    f.write(content)
