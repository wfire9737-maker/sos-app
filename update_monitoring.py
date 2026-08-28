import re

with open("app/src/main/java/com/example/ui/screens/DeviceMonitoringScreen.kt", "r") as f:
    content = f.read()

# Add states for battery and RSSI
state_add = """    val bleBatteryLevel by viewModel.bleBatteryLevel.collectAsState()
    val bleBatteryDisplay by viewModel.bleBatteryDisplay.collectAsState()
    val bleRssi by viewModel.bleRssi.collectAsState()

    var selectedDeviceId by remember { mutableStateOf(devices.firstOrNull()?.deviceId) }"""

content = re.sub(r'var selectedDeviceId by remember \{ mutableStateOf\(devices\.firstOrNull\(\)\?\.deviceId\) \}', state_add, content)

# Change signature of DeviceStatusHeader and HardwareMetricsGrid
content = content.replace("fun DeviceStatusHeader(device: Device, isEsp32Connected: Boolean) {",
                          "fun DeviceStatusHeader(device: Device, isEsp32Connected: Boolean, bleBatteryLevel: Int?, bleRssi: Int?) {")

content = content.replace("DeviceStatusHeader(device = device, isEsp32Connected = isEsp32Connected)",
                          "DeviceStatusHeader(device = device, isEsp32Connected = isEsp32Connected, bleBatteryLevel = bleBatteryLevel, bleRssi = bleRssi)")

content = content.replace("fun HardwareMetricsGrid(device: Device) {",
                          "fun HardwareMetricsGrid(device: Device, isEsp32Connected: Boolean, bleBatteryLevel: Int?, bleRssi: Int?, bleBatteryDisplay: String) {")

content = content.replace("HardwareMetricsGrid(device = device)",
                          "HardwareMetricsGrid(device = device, isEsp32Connected = isEsp32Connected, bleBatteryLevel = bleBatteryLevel, bleRssi = bleRssi, bleBatteryDisplay = bleBatteryDisplay)")

# In HardwareMetricsGrid
grid_replacement = """                            value = if (!isEsp32Connected) "--" else bleBatteryDisplay,
                            icon = if (!isEsp32Connected) Icons.AutoMirrored.Filled.BatteryUnknown else Icons.Default.BatteryFull,
                            color = if (!isEsp32Connected) Color.Gray else if ((bleBatteryLevel ?: 0) > 20) SafetyGreen else EmergencyRed,
                        )
                    }
                    item {
                        MetricCard(
                            title = "Signal Strength",
                            value = if (!isEsp32Connected) "--" else "${bleRssi ?: "--"} dBm",
                            icon = Icons.Default.Bluetooth,
                            color = if (!isEsp32Connected) Color.Gray else Color(0xFF1565C0),"""
content = re.sub(
    r'value = if \(device\.status == "DISCONNECTED"\) "--" else "\$\{device\.batteryLevel\}%",\s*icon = if \(device\.status == "DISCONNECTED"\) Icons\.AutoMirrored\.Filled\.BatteryUnknown else Icons\.Default\.BatteryFull,\s*color = if \(device\.status == "DISCONNECTED"\) Color\.Gray else if \(device\.batteryLevel > 20\) SafetyGreen else EmergencyRed,\s*\)\s*\}\s*item \{\s*MetricCard\(\s*title = "Signal Strength",\s*value = if \(device\.status == "DISCONNECTED"\) "--" else "\$\{device\.signalStrength\} dBm",\s*icon = Icons\.Default\.Bluetooth,\s*color = if \(device\.status == "DISCONNECTED"\) Color\.Gray else Color\(0xFF1565C0\),',
    grid_replacement,
    content
)

with open("app/src/main/java/com/example/ui/screens/DeviceMonitoringScreen.kt", "w") as f:
    f.write(content)
