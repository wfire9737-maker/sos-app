import re

with open("app/src/main/java/com/example/ui/GuardianViewModel.kt", "r") as f:
    content = f.read()

props = """    val bleBatteryDisplay = deviceService.bleManager.batteryDisplay
    val bleBatteryLevel = deviceService.bleManager.batteryLevel
    val bleRssi = deviceService.bleManager.rssi
"""

if "val bleBatteryDisplay" not in content:
    content = content.replace("val mpuNotificationSubscribed = deviceService.bleManager.mpuNotificationSubscribed",
                              "val mpuNotificationSubscribed = deviceService.bleManager.mpuNotificationSubscribed\n" + props)

with open("app/src/main/java/com/example/ui/GuardianViewModel.kt", "w") as f:
    f.write(content)
