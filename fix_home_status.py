import os

filepath = "app/src/main/java/com/example/ui/screens/HomeScreen.kt"
with open(filepath, "r") as f:
    content = f.read()

old_status = """    val maxBattery = devices.maxOfOrNull { it.batteryLevel } ?: 0
    
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        StatusCard(
            modifier = Modifier.weight(1f),
            icon = Icons.Default.Bluetooth,
            label = "Bluetooth",
            value = if (isBleConnected) "Connected" else "Not Paired",
            statusColor = if (isBleConnected) SafetyGreen else MaterialTheme.colorScheme.onSurfaceVariant
        )
        StatusCard(
            modifier = Modifier.weight(1f),
            icon = Icons.Default.BatteryFull,
            label = "Battery",
            value = if (devices.isNotEmpty()) "${maxBattery}%" else "--",
            statusColor = if (maxBattery > 20) SafetyGreen else if (maxBattery > 0) AlertOrange else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }"""
new_status = """    val maxBattery = devices.maxOfOrNull { it.batteryLevel } ?: 0
    
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatusCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.Bluetooth,
                label = "Bluetooth",
                value = if (isBleConnected) "Connected" else "Not Paired",
                statusColor = if (isBleConnected) SafetyGreen else MaterialTheme.colorScheme.onSurfaceVariant
            )
            StatusCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.BatteryFull,
                label = "Battery",
                value = if (devices.isNotEmpty()) "${maxBattery}%" else "--",
                statusColor = if (maxBattery > 20) SafetyGreen else if (maxBattery > 0) AlertOrange else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatusCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.LocationOn,
                label = "GPS",
                value = "Active",
                statusColor = SafetyGreen
            )
            StatusCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.Wifi,
                label = "Internet",
                value = "Connected",
                statusColor = SafetyGreen
            )
        }
    }"""
content = content.replace(old_status, new_status)

with open(filepath, "w") as f:
    f.write(content)
