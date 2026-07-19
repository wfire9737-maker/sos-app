package com.example.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Device
import com.example.ui.GuardianViewModel
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceMonitoringScreen(
    viewModel: GuardianViewModel,
    onNavigateBack: () -> Unit
) {
    val devices by viewModel.devices.collectAsState()
    
    // Select the first device to monitor if available
    var selectedDeviceId by remember { mutableStateOf(devices.firstOrNull()?.deviceId) }
    val device = devices.find { it.deviceId == selectedDeviceId }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Device Diagnostics", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.refreshDeviceStatus() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        }
    ) { paddingValues ->
        if (device == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.WatchOff, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("No devices connected.", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Device Selector
                item {
                    ScrollableTabRow(
                        selectedTabIndex = devices.indexOfFirst { it.deviceId == selectedDeviceId }.coerceAtLeast(0),
                        edgePadding = 0.dp,
                        containerColor = Color.Transparent
                    ) {
                        devices.forEachIndexed { index, d ->
                            Tab(
                                selected = d.deviceId == selectedDeviceId,
                                onClick = { selectedDeviceId = d.deviceId },
                                text = { Text(d.deviceName) }
                            )
                        }
                    }
                }
                
                // Status Header
                item {
                    DeviceStatusHeader(device = device)
                }
                
                // Telemetry Grid
                item {
                    Text("Live Telemetry", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        TelemetryCard(
                            title = "Battery",
                            value = "${device.batteryLevel}%",
                            icon = Icons.Default.BatteryFull,
                            color = if (device.batteryLevel > 20) SafetyGreen else EmergencyRed,
                            modifier = Modifier.weight(1f)
                        )
                        TelemetryCard(
                            title = "Signal",
                            value = "${device.signalStrength} dBm",
                            icon = Icons.Default.SignalCellularAlt,
                            color = Color(0xFF1565C0),
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        TelemetryCard(
                            title = "Temp",
                            value = "${device.deviceTemperature}°C",
                            icon = Icons.Default.Thermostat,
                            color = AlertOrange,
                            modifier = Modifier.weight(1f)
                        )
                        TelemetryCard(
                            title = "CPU",
                            value = "${device.cpuUsagePercent}%",
                            icon = Icons.Default.Memory,
                            color = Color(0xFF00BCD4),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                
                // MPU Data
                item {
                    Text("Motion Sensors (MPU6050)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Accelerometer (g)", fontWeight = FontWeight.Bold)
                            Text("X: ${device.accelX} | Y: ${device.accelY} | Z: ${device.accelZ}", fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Gyroscope (°/s)", fontWeight = FontWeight.Bold)
                            Text("X: ${device.gyroX} | Y: ${device.gyroY} | Z: ${device.gyroZ}", fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                        }
                    }
                }

                // Controls
                item {
                    Text("Diagnostics & Controls", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(
                            onClick = { viewModel.triggerManualHeartbeatCheck(device.deviceId) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Ping")
                        }
                        OutlinedButton(
                            onClick = { viewModel.restartDevice(device.deviceId) },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text("Reboot")
                        }
                    }
                }
                
                item { Spacer(modifier = Modifier.height(32.dp)) }
            }
        }
    }
}

@Composable
fun DeviceStatusHeader(device: Device) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(
                        if (device.status == "CONNECTED") SafetyGreen.copy(alpha = 0.2f) else EmergencyRed.copy(alpha = 0.2f),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Watch,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = if (device.status == "CONNECTED") SafetyGreen else EmergencyRed
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(device.deviceName, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                Text("MAC: ${device.macAddress}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Firmware: ${device.firmwareVersion}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun TelemetryCard(title: String, value: String, icon: ImageVector, color: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.height(100.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp).fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text(title, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            }
            Text(value, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        }
    }
}
