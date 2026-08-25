import re

with open('app/src/main/java/com/example/ui/screens/HomeScreen.kt', 'r') as f:
    content = f.read()

target1 = "    onNavigateToMap: () -> Unit = {},"
replacement1 = "    onNavigateToMap: () -> Unit = {},\n    onNavigateToBleTest: () -> Unit = {},"

content = content.replace(target1, replacement1)

target2 = """            HomeMenuItem(
                title = "Device Monitoring",
                icon = Icons.Default.Bluetooth,
                onClick = onNavigateToDeviceMonitoring
            )"""

replacement2 = """            HomeMenuItem(
                title = "Device Monitoring",
                icon = Icons.Default.Bluetooth,
                onClick = onNavigateToDeviceMonitoring
            )
            HomeMenuItem(
                title = "Raw BLE Connection Test",
                icon = Icons.Default.BluetoothConnected,
                onClick = onNavigateToBleTest
            )"""

content = content.replace(target2, replacement2)

with open('app/src/main/java/com/example/ui/screens/HomeScreen.kt', 'w') as f:
    f.write(content)
