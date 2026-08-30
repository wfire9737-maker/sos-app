import re

with open("app/src/main/java/com/example/ui/navigation/Screen.kt", "r") as f:
    content = f.read()

target = '    object BleTest : Screen("ble_test")'
replacement = '    object BleTest : Screen("ble_test")\n    object NearbyDiscovery : Screen("nearby_discovery")'

content = content.replace(target, replacement)

with open("app/src/main/java/com/example/ui/navigation/Screen.kt", "w") as f:
    f.write(content)
