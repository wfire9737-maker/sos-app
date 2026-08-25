import re

with open('app/src/main/java/com/example/ui/screens/HomeScreen.kt', 'r') as f:
    content = f.read()

target_grid_usage = "                StatusGrid(devices = devices)"
replacement_grid_usage = "                StatusGrid(devices = devices, onBluetoothClick = onNavigateToBleTest)"
content = content.replace(target_grid_usage, replacement_grid_usage)

target_grid_def = "fun StatusGrid(devices: List<Device>) {"
replacement_grid_def = "fun StatusGrid(devices: List<Device>, onBluetoothClick: () -> Unit = {}) {"
content = content.replace(target_grid_def, replacement_grid_def)

target_card_call = """        StatusCard(
            modifier = Modifier.weight(1f),
            icon = Icons.Default.Bluetooth,
            label = "Bluetooth","""

replacement_card_call = """        StatusCard(
            modifier = Modifier.weight(1f).clickable { onBluetoothClick() },
            icon = Icons.Default.Bluetooth,
            label = "Bluetooth","""

content = content.replace(target_card_call, replacement_card_call)

with open('app/src/main/java/com/example/ui/screens/HomeScreen.kt', 'w') as f:
    f.write(content)
