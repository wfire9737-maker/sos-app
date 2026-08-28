with open("app/src/main/java/com/example/service/EmergencyService.kt", "r") as f:
    lines = f.readlines()

new_lines = []
skip = False
for line in lines:
    if "val locationStr = if (model.latitude != 0.0" in line:
        skip = True
        new_lines.append("""            val locationStr = if (model.latitude != 0.0 && model.longitude != 0.0) {
                "Location:\\nhttps://maps.google.com/?q=${model.latitude},${model.longitude}"
            } else {
                "Location: UNAVAILABLE"
            }
            val message = if (isUpdate) {
                "LIVE UPDATE!\\n${model.userName} is still in an active emergency.\\n\\n$locationStr\\n\\nTime: $timestamp"
            } else {
                "EMERGENCY!\\n${model.userName} has triggered an SOS.\\n\\n$locationStr\\n\\nPlease contact immediately.\\n\\nTime: $timestamp"
            }\n""")
    elif "try {" in line and skip:
        skip = False
        new_lines.append(line)
    elif not skip:
        new_lines.append(line)

with open("app/src/main/java/com/example/service/EmergencyService.kt", "w") as f:
    f.writelines(new_lines)
