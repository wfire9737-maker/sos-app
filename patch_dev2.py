import re

with open("app/src/main/java/com/example/ui/screens/DeveloperDashboardScreen.kt", "r") as f:
    content = f.read()

sim_buttons = """
            Spacer(modifier = Modifier.height(16.dp))
            Text("Bluetooth Simulator", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { 
                    firstDevice?.deviceId?.let { viewModel.connectSimulatedDevice(it) }
                }, modifier = Modifier.weight(1f)) {
                    Text("Connect Device")
                }
                Button(onClick = { 
                    firstDevice?.deviceId?.let { viewModel.disconnectSimulatedDevice(it) }
                }, modifier = Modifier.weight(1f)) {
                    Text("Disconnect Device")
                }
            }

            Button(onClick = { 
                firstDevice?.deviceId?.let { viewModel.triggerEsp32IncomingSos(it, "ESP32_BUTTON") }
            }, modifier = Modifier.fillMaxWidth()) {
                Text("Send SOS_PRESS")
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { 
                    firstDevice?.deviceId?.let { viewModel.connectSimulatedDevice(it) }
                }, modifier = Modifier.weight(1f)) {
                    Text("Send DEVICE_CONNECTED")
                }
                Button(onClick = { 
                    firstDevice?.deviceId?.let { viewModel.disconnectSimulatedDevice(it) }
                }, modifier = Modifier.weight(1f)) {
                    Text("Send DEVICE_DISCONNECTED")
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { 
                    firstDevice?.let { 
                        viewModel.sendSimulatedTelemetry(
                            deviceId = it.deviceId,
                            battery = 85,
                            isCharging = false,
                            latitude = it.latitude,
                            longitude = it.longitude,
                            ax = it.accelX, ay = it.accelY, az = it.accelZ,
                            gx = it.gyroX, gy = it.gyroY, gz = it.gyroZ,
                            firmware = it.firmwareVersion
                        )
                    }
                }, modifier = Modifier.weight(1f)) {
                    Text("Send BATTERY:85")
                }
                Button(onClick = { 
                    firstDevice?.deviceId?.let { viewModel.triggerManualHeartbeatCheck(it) }
                }, modifier = Modifier.weight(1f)) {
                    Text("Send HEARTBEAT")
                }
            }
"""

content = content.replace('                Text("Simulate SOS Button Press", color = Color.White)\n            }', '                Text("Simulate SOS Button Press", color = Color.White)\n            }\n' + sim_buttons)

# also since we are adding a lot of UI, change Column to LazyColumn for scrollability in DeveloperDashboardScreen
content = content.replace("Column(\n            modifier = Modifier\n                .fillMaxSize()\n                .padding(paddingValues)\n                .padding(16.dp),", "androidx.compose.foundation.lazy.LazyColumn(\n            modifier = Modifier\n                .fillMaxSize()\n                .padding(paddingValues)\n                .padding(16.dp),")
# wait, lazy column requires `item { ... }`.
