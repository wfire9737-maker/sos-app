import re

with open("app/src/main/java/com/example/ui/GuardianViewModel.kt", "r") as f:
    content = f.read()

content = content.replace(
    "val isEsp32Connected = deviceService.isEsp32Connected",
    "val isEsp32Connected = deviceService.isEsp32Connected\n    val bleConnectionState = deviceService.bleManager.connectionState"
)

with open("app/src/main/java/com/example/ui/GuardianViewModel.kt", "w") as f:
    f.write(content)
