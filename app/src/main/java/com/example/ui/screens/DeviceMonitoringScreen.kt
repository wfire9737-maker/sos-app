package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Device
import com.example.ui.GuardianViewModel
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceMonitoringScreen(
    viewModel: GuardianViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val devices by viewModel.devices.collectAsState()
    val isRefreshing by viewModel.isRefreshingDevices.collectAsState()
    val isDiagnosing by viewModel.isDiagnosingDevice.collectAsState()
    val diagnosticsLog by viewModel.diagnosticsLog.collectAsState()

    var selectedDeviceId by remember(devices) {
        mutableStateOf(devices.firstOrNull()?.deviceId ?: "")
    }

    val selectedDevice = remember(devices, selectedDeviceId) {
        devices.find { it.deviceId == selectedDeviceId } ?: devices.firstOrNull()
    }

    var showRenameDialog by remember { mutableStateOf(false) }
    var renameDeviceName by remember { mutableStateOf("") }

    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Device Health Center",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "Telemetry & Diagnostics Panel",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("device_monitoring_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Navigate Back",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                },
                actions = {
                    // Refreshes status
                    IconButton(
                        onClick = { viewModel.refreshDeviceStatus() },
                        enabled = !isRefreshing && !isDiagnosing,
                        modifier = Modifier.testTag("device_monitoring_refresh_button")
                    ) {
                        val rotation = rememberInfiniteTransition(label = "refreshRotation")
                        val angle by rotation.animateFloat(
                            initialValue = 0f,
                            targetValue = 360f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(1200, easing = LinearEasing),
                                repeatMode = RepeatMode.Restart
                            ),
                            label = "angle"
                        )
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Sync Telemetry",
                            modifier = if (isRefreshing) Modifier.rotate(angle) else Modifier
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (isRefreshing && devices.isEmpty()) {
                // Skeleton loading state
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item { Spacer(modifier = Modifier.height(12.dp)) }
                    item {
                        SkeletonLine(widthFraction = 0.4f, height = 18.dp)
                    }
                    items(3) {
                        SkeletonCard(height = 120.dp, borderRadius = 20.dp)
                    }
                }
            } else if (devices.isEmpty()) {
                // Empty state
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp)
                        .testTag("no_devices_empty_state"),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Watch,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(50.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No Registered Devices Found",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Bond an ESP32 wearable or pair a hardware beacon from the home screen to access diagnostic metrics.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    // Horizontal device switcher row
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "Select Monitored Node:",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                devices.forEach { dev ->
                                    val isSelected = dev.deviceId == selectedDeviceId
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { selectedDeviceId = dev.deviceId },
                                        label = {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(8.dp)
                                                        .background(
                                                            color = when (dev.status) {
                                                                "CONNECTED" -> SafetyGreen
                                                                "REBOOTING" -> AlertOrange
                                                                else -> Color.Gray
                                                            },
                                                            shape = CircleShape
                                                        )
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(dev.deviceName)
                                            }
                                        },
                                        modifier = Modifier.testTag("device_chip_${dev.deviceId}")
                                    )
                                }
                            }
                        }
                    }

                    selectedDevice?.let { device ->
                        // Header info card (Name, MAC, Status, edit name button)
                        item {
                            Card(
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f), RoundedCornerShape(20.dp))
                                    .testTag("device_header_card")
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(52.dp)
                                            .background(
                                                color = when (device.status) {
                                                    "CONNECTED" -> SafetyGreen.copy(alpha = 0.15f)
                                                    "REBOOTING" -> AlertOrange.copy(alpha = 0.15f)
                                                    else -> MaterialTheme.colorScheme.error.copy(alpha = 0.1f)
                                                },
                                                shape = CircleShape
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Watch,
                                            contentDescription = null,
                                            tint = when (device.status) {
                                                "CONNECTED" -> SafetyGreen
                                                "REBOOTING" -> AlertOrange
                                                else -> MaterialTheme.colorScheme.error
                                            },
                                            modifier = Modifier.size(28.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(16.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = device.deviceName,
                                                fontSize = 17.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            IconButton(
                                                onClick = {
                                                    renameDeviceName = device.deviceName
                                                    showRenameDialog = true
                                                },
                                                modifier = Modifier.size(24.dp).testTag("device_rename_icon")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Edit,
                                                    contentDescription = "Rename Nickname",
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }
                                        }
                                        Text(
                                            text = "Node ID: ${device.deviceId}",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = "MAC Addr: ${device.macAddress}",
                                            fontSize = 11.sp,
                                            fontFamily = FontFamily.Monospace,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                        )
                                    }

                                    Column(horizontalAlignment = Alignment.End) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(
                                                    color = when (device.status) {
                                                        "CONNECTED" -> SafetyGreen.copy(alpha = 0.15f)
                                                        "REBOOTING" -> AlertOrange.copy(alpha = 0.15f)
                                                        else -> MaterialTheme.colorScheme.error.copy(alpha = 0.15f)
                                                    }
                                                )
                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Text(
                                                text = device.status,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = when (device.status) {
                                                    "CONNECTED" -> SafetyGreen
                                                    "REBOOTING" -> AlertOrange
                                                    else -> MaterialTheme.colorScheme.error
                                                }
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "Firmware: ${device.firmwareVersion}",
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                        )
                                    }
                                }
                            }
                        }

                        // System Health Score Arc/Gauge Card
                        item {
                            Card(
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.08f), RoundedCornerShape(20.dp))
                                    .testTag("device_health_card")
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(20.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "System Integrity Status",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Dynamic health score computed from sensors, cell health, & rf latency",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center
                                    )

                                    Spacer(modifier = Modifier.height(20.dp))

                                    // Gauge Canvas
                                    Box(
                                        modifier = Modifier.size(160.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        val animatedScore = animateFloatAsState(
                                            targetValue = device.healthScore.toFloat(),
                                            animationSpec = tween(1500, easing = FastOutSlowInEasing),
                                            label = "healthScoreAnimation"
                                        )

                                        val strokeColor = when {
                                            device.healthScore >= 85 -> SafetyGreen
                                            device.healthScore >= 65 -> AlertOrange
                                            else -> MaterialTheme.colorScheme.error
                                        }

                                        Canvas(modifier = Modifier.fillMaxSize()) {
                                            // Back Arc (gray track)
                                            drawArc(
                                                color = Color(0xFFE3E2E6),
                                                startAngle = 135f,
                                                sweepAngle = 270f,
                                                useCenter = false,
                                                style = Stroke(width = 14.dp.toPx(), cap = StrokeCap.Round)
                                            )
                                            // Active Arc
                                            drawArc(
                                                color = strokeColor,
                                                startAngle = 135f,
                                                sweepAngle = (animatedScore.value / 100f) * 270f,
                                                useCenter = false,
                                                style = Stroke(width = 14.dp.toPx(), cap = StrokeCap.Round)
                                            )
                                        }

                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(
                                                text = "${device.healthScore}%",
                                                fontSize = 32.sp,
                                                fontWeight = FontWeight.Black,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = device.deviceHealth,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = strokeColor
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        IndicatorBadge(
                                            label = "Connection",
                                            value = device.connectionStatus,
                                            success = device.connectionStatus == "ONLINE"
                                        )
                                        IndicatorBadge(
                                            label = "Bluetooth",
                                            value = device.bluetoothStatus,
                                            success = device.bluetoothStatus == "CONNECTED"
                                        )
                                    }
                                }
                            }
                        }

                        // Telemetry Metrics Grid Section
                        item {
                            Text(
                                text = "Real-Time Node Telemetry",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    TelemetryMetricCard(
                                        title = "Battery Power",
                                        value = "${device.batteryLevel}%",
                                        subValue = if (device.isCharging) "🔌 Charging Active" else "🔋 Discharging",
                                        icon = if (device.isCharging) Icons.Default.BatteryChargingFull else Icons.Default.BatteryFull,
                                        accentColor = if (device.batteryLevel < 20) MaterialTheme.colorScheme.error else SafetyGreen,
                                        modifier = Modifier.weight(1f)
                                    )
                                    TelemetryMetricCard(
                                        title = "Wi-Fi Signal",
                                        value = "${device.wifiSignal} dBm",
                                        subValue = when {
                                            device.wifiSignal >= -60 -> "Signal: Strong"
                                            device.wifiSignal >= -80 -> "Signal: Moderate"
                                            else -> "Signal: Poor"
                                        },
                                        icon = Icons.Default.Wifi,
                                        accentColor = if (device.wifiSignal < -80) AlertOrange else SafetyBlue,
                                        modifier = Modifier.weight(1f)
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    TelemetryMetricCard(
                                        title = "Node Temperature",
                                        value = String.format("%.1f °C", device.deviceTemperature),
                                        subValue = if (device.deviceTemperature > 38.5f) "⚠️ High Thermal" else "🟢 Thermal Normal",
                                        icon = Icons.Default.Thermostat,
                                        accentColor = if (device.deviceTemperature > 38.5f) AlertOrange else SafetyGreen,
                                        modifier = Modifier.weight(1f)
                                    )
                                    TelemetryMetricCard(
                                        title = "Device GPS",
                                        value = device.gpsStatus,
                                        subValue = if (device.gpsStatus == "LOCKED") "🎯 High Accuracy" else "🛰️ Finding Satellites",
                                        icon = Icons.Default.GpsFixed,
                                        accentColor = if (device.gpsStatus == "LOCKED") SafetyGreen else AlertOrange,
                                        modifier = Modifier.weight(1f)
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    // Memory Usage progress bar
                                    TelemetryGaugeCard(
                                        title = "Allocated SRAM",
                                        percent = device.memoryUsagePercent,
                                        icon = Icons.Default.Memory,
                                        accentColor = SafetyBlue,
                                        modifier = Modifier.weight(1f)
                                    )
                                    // CPU Usage progress bar
                                    TelemetryGaugeCard(
                                        title = "Core CPU Load",
                                        percent = device.cpuUsagePercent,
                                        icon = Icons.Default.DeveloperBoard,
                                        accentColor = AlertOrange,
                                        modifier = Modifier.weight(1f)
                                    )
                                }

                                // Uptime and Last Online Card
                                Card(
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                text = "System Uptime",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                fontWeight = FontWeight.Bold
                                            )
                                            val upDays = device.uptimeSeconds / 86400
                                            val upHours = (device.uptimeSeconds % 86400) / 3600
                                            val upMins = (device.uptimeSeconds % 3600) / 60
                                            val upSecs = device.uptimeSeconds % 60
                                            val uptimeStr = if (upDays > 0) {
                                                "${upDays}d ${upHours}h ${upMins}m"
                                            } else {
                                                "${upHours}h ${upMins}m ${upSecs}s"
                                            }
                                            Text(
                                                text = uptimeStr,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }

                                        Divider(
                                            modifier = Modifier
                                                .height(32.dp)
                                                .width(1.dp),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.12f)
                                        )

                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(
                                                text = "Heartbeat Sync",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                fontWeight = FontWeight.Bold
                                            )
                                            val timeDiff = System.currentTimeMillis() - device.lastSync
                                            val timeDiffSec = timeDiff / 1000
                                            val syncStr = when {
                                                timeDiffSec < 1 -> "Just now"
                                                timeDiffSec < 60 -> "$timeDiffSec sec ago"
                                                else -> "${timeDiffSec / 60} min ago"
                                            }
                                            Text(
                                                text = syncStr,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Core Action Controls (Reboot, Diagnostics)
                        item {
                            Text(
                                text = "System Administration",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // Reboot Node Button
                                Button(
                                    onClick = { viewModel.restartDevice(device.deviceId) },
                                    enabled = !isDiagnosing && device.status != "REBOOTING",
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.errorContainer,
                                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .testTag("reboot_device_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.RestartAlt,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Reboot Node", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }

                                // Run Self-Test Button
                                Button(
                                    onClick = { viewModel.runDiagnostics(device.deviceId) },
                                    enabled = !isDiagnosing && device.status == "CONNECTED",
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .testTag("run_diagnostics_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Construction,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Run Self-Test", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        // Monospace scrolling diagnostic terminal
                        if (diagnosticsLog.isNotEmpty() || isDiagnosing) {
                            item {
                                Text(
                                    text = "Diagnostic Readout Console",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            item {
                                Card(
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(240.dp)
                                        .border(1.dp, Color(0xFF333333), RoundedCornerShape(12.dp))
                                        .testTag("diagnostics_terminal_console")
                                ) {
                                    Box(modifier = Modifier.fillMaxSize().padding(12.dp)) {
                                        val scrollState = rememberScrollState()
                                        
                                        // Auto-scroll terminal down
                                        LaunchedEffect(diagnosticsLog.size) {
                                            scrollState.animateScrollTo(scrollState.maxValue)
                                        }

                                        Column(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .verticalScroll(scrollState),
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            diagnosticsLog.forEach { line ->
                                                val lineSpanColor = when {
                                                    line.startsWith("❌") -> Color(0xFFFF6B6B)
                                                    line.startsWith("✅") -> Color(0xFF51CF66)
                                                    line.startsWith("⚠️") -> Color(0xFFFCC419)
                                                    line.startsWith("📋") -> Color(0xFF339AF0)
                                                    else -> Color(0xFFE0E0E0)
                                                }
                                                Text(
                                                    text = line,
                                                    fontSize = 11.sp,
                                                    fontFamily = FontFamily.Monospace,
                                                    color = lineSpanColor,
                                                    modifier = Modifier.fillMaxWidth()
                                                )
                                            }
                                            if (isDiagnosing) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier.padding(top = 4.dp)
                                                ) {
                                                    val infiniteTransition = rememberInfiniteTransition(label = "terminalLoading")
                                                    val rotationAngle by infiniteTransition.animateFloat(
                                                        initialValue = 0f,
                                                        targetValue = 360f,
                                                        animationSpec = infiniteRepeatable(
                                                            animation = tween(1000, easing = LinearEasing)
                                                        ),
                                                        label = "angle"
                                                    )
                                                    Icon(
                                                        imageVector = Icons.Default.Autorenew,
                                                        contentDescription = null,
                                                        tint = Color(0xFF339AF0),
                                                        modifier = Modifier.size(12.dp).rotate(rotationAngle)
                                                    )
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text(
                                                        text = "Analyzing registry mapping and noise floor...",
                                                        fontSize = 11.sp,
                                                        fontFamily = FontFamily.Monospace,
                                                        color = Color(0xFF339AF0)
                                                    )
                                                }
                                            }
                                        }
                                        
                                        // Top Right "Clear" action
                                        if (diagnosticsLog.isNotEmpty() && !isDiagnosing) {
                                            IconButton(
                                                onClick = { viewModel.cleanDiagnosticsLog() },
                                                modifier = Modifier
                                                    .align(Alignment.TopEnd)
                                                    .size(24.dp)
                                                    .background(Color.White.copy(alpha = 0.15f), CircleShape)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Close,
                                                    contentDescription = "Clear logs",
                                                    tint = Color.White,
                                                    modifier = Modifier.size(12.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // --- MODULE 16: ESP32 LAB CONTROLS ---
                    item {
                        Divider(
                            modifier = Modifier.padding(vertical = 12.dp),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "ESP32 Handshake & Telemetry Lab",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    // Network Offline/Online simulation card
                    item {
                        val isNetworkOn by viewModel.isNetworkAvailable.collectAsState()
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isNetworkOn) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.15f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(
                                    width = 1.dp,
                                    color = if (isNetworkOn) MaterialTheme.colorScheme.outline.copy(alpha = 0.08f) else MaterialTheme.colorScheme.error.copy(alpha = 0.3f),
                                    shape = RoundedCornerShape(16.dp)
                                )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Wireless Gateway Connectivity",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isNetworkOn) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onErrorContainer
                                    )
                                    Text(
                                        text = if (isNetworkOn) "🟢 Network Online. UDP packets flow normally." else "🔴 Gateway Offline. Incoming telemetry will buffer locally.",
                                        fontSize = 11.sp,
                                        color = if (isNetworkOn) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
                                    )
                                }
                                Switch(
                                    checked = isNetworkOn,
                                    onCheckedChange = { viewModel.setNetworkAvailable(it) },
                                    thumbContent = {
                                        Icon(
                                            imageVector = if (isNetworkOn) Icons.Default.Wifi else Icons.Default.WifiOff,
                                            contentDescription = null,
                                            modifier = Modifier.size(12.dp)
                                        )
                                    }
                                )
                            }
                        }
                    }

                    // Real-Time packet stream log
                    item {
                        val commLogs by viewModel.esp32CommLogs.collectAsState()
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF151515)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp)
                                .border(1.dp, Color(0xFF262626), RoundedCornerShape(12.dp))
                        ) {
                            Box(modifier = Modifier.fillMaxSize().padding(12.dp)) {
                                Column(modifier = Modifier.fillMaxSize()) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "🛰️ ESP32 UDP Broadcast Packets",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace,
                                            color = SafetyGreen
                                        )
                                        if (commLogs.isNotEmpty()) {
                                            TextButton(
                                                onClick = { viewModel.clearCommLogs() },
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                                modifier = Modifier.height(24.dp)
                                            ) {
                                                Text("Clear", fontSize = 10.sp, color = Color.Gray, fontFamily = FontFamily.Monospace)
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    if (commLogs.isEmpty()) {
                                        Box(
                                            modifier = Modifier.fillMaxSize(),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "[Listening on UDP Broadcast Port 8888...]",
                                                fontSize = 10.sp,
                                                fontFamily = FontFamily.Monospace,
                                                color = Color.DarkGray
                                            )
                                        }
                                    } else {
                                        LazyColumn(
                                            modifier = Modifier.fillMaxSize(),
                                            verticalArrangement = Arrangement.spacedBy(2.dp)
                                        ) {
                                            items(commLogs) { log ->
                                                Text(
                                                    text = log,
                                                    fontSize = 10.sp,
                                                    fontFamily = FontFamily.Monospace,
                                                    color = when {
                                                        log.contains("❌") || log.contains("🚨") || log.contains("💥") -> Color(0xFFFF6B6B)
                                                        log.contains("🟢") || log.contains("✅") || log.contains("SUCCESS") -> Color(0xFF51CF66)
                                                        log.contains("⚠️") -> Color(0xFFFCC419)
                                                        log.contains("💓") || log.contains("Ping") -> Color(0xFFE599F7)
                                                        else -> Color(0xFFADB5BD)
                                                    },
                                                    modifier = Modifier.fillMaxWidth()
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    selectedDevice?.let { device ->
                        // Sliders and telemetry inputs for the active node
                        item {
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Text(
                                        text = "Stream Telemetry for ${device.deviceName}",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )

                                    // Battery level slider
                                    var simBattery by remember { mutableStateOf(device.batteryLevel.toFloat()) }
                                    var simCharging by remember { mutableStateOf(device.isCharging) }
                                    
                                    Column {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("Battery Charge Level: ${simBattery.toInt()}%", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Checkbox(
                                                    checked = simCharging,
                                                    onCheckedChange = { simCharging = it },
                                                    modifier = Modifier.size(24.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("USB Charging", fontSize = 11.sp)
                                            }
                                        }
                                        Slider(
                                            value = simBattery,
                                            onValueChange = { simBattery = it },
                                            valueRange = 0f..100f,
                                            modifier = Modifier.height(28.dp)
                                        )
                                    }

                                    // GPS coordinates simulations
                                    var simLat by remember { mutableStateOf(device.latitude) }
                                    var simLng by remember { mutableStateOf(device.longitude) }

                                    Column {
                                        Text("Coordinates: (${String.format("%.4f", simLat)}, ${String.format("%.4f", simLng)})", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            modifier = Modifier.padding(top = 4.dp)
                                        ) {
                                            Button(
                                                onClick = { simLat += 0.0015 },
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurfaceVariant),
                                                modifier = Modifier.height(28.dp).weight(1f)
                                            ) {
                                                Text("Lat +0.0015", fontSize = 10.sp)
                                            }
                                            Button(
                                                onClick = { simLng += 0.0015 },
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurfaceVariant),
                                                modifier = Modifier.height(28.dp).weight(1f)
                                            ) {
                                                Text("Lng +0.0015", fontSize = 10.sp)
                                            }
                                            Button(
                                                onClick = { 
                                                    simLat = 37.7749
                                                    simLng = -122.4194
                                                },
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurfaceVariant),
                                                modifier = Modifier.height(28.dp).weight(1f)
                                            ) {
                                                Text("Reset GPS", fontSize = 10.sp)
                                            }
                                        }
                                    }

                                    // MPU6050 settings
                                    var ax by remember { mutableStateOf(device.accelX) }
                                    var ay by remember { mutableStateOf(device.accelY) }
                                    var az by remember { mutableStateOf(device.accelZ) }

                                    Column {
                                        Text("MPU6050 Accelerometer Forces: (G)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text("Ax: ${String.format("%.2f", ax)}G", fontSize = 9.sp, color = Color.Gray)
                                                Slider(
                                                    value = ax,
                                                    onValueChange = { ax = it },
                                                    valueRange = -6f..6f,
                                                    modifier = Modifier.height(24.dp)
                                                )
                                            }
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text("Ay: ${String.format("%.2f", ay)}G", fontSize = 9.sp, color = Color.Gray)
                                                Slider(
                                                    value = ay,
                                                    onValueChange = { ay = it },
                                                    valueRange = -6f..6f,
                                                    modifier = Modifier.height(24.dp)
                                                )
                                            }
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text("Az: ${String.format("%.2f", az)}G", fontSize = 9.sp, color = Color.Gray)
                                                Slider(
                                                    value = az,
                                                    onValueChange = { az = it },
                                                    valueRange = -6f..6f,
                                                    modifier = Modifier.height(24.dp)
                                                )
                                            }
                                        }
                                    }

                                    // Action deck for active simulations
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.fillMaxWidth().padding(top = 6.dp)
                                    ) {
                                        Button(
                                            onClick = {
                                                viewModel.sendSimulatedTelemetry(
                                                    deviceId = device.deviceId,
                                                    battery = simBattery.toInt(),
                                                    isCharging = simCharging,
                                                    latitude = simLat,
                                                    longitude = simLng,
                                                    ax = ax, ay = ay, az = az,
                                                    gx = 0.1f, gy = -0.1f, gz = 0.2f,
                                                    firmware = device.firmwareVersion
                                                )
                                            },
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.height(36.dp).weight(1f)
                                        ) {
                                            Text("Send Telemetry", fontSize = 11.sp)
                                        }

                                        Button(
                                            onClick = {
                                                ax = 0.12f
                                                ay = -0.2f
                                                az = 5.2f // triggers G-force threshold fall (> 4.5G)
                                                viewModel.sendSimulatedTelemetry(
                                                    deviceId = device.deviceId,
                                                    battery = simBattery.toInt(),
                                                    isCharging = simCharging,
                                                    latitude = simLat,
                                                    longitude = simLng,
                                                    ax = 0.12f, ay = -0.2f, az = 5.2f,
                                                    gx = 2.4f, gy = 3.1f, gz = 1.8f,
                                                    firmware = device.firmwareVersion
                                                )
                                            },
                                            shape = RoundedCornerShape(8.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                            modifier = Modifier.height(36.dp).weight(1.2f)
                                        ) {
                                            Text("Simulate Falling 💥", fontSize = 11.sp)
                                        }
                                    }

                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Button(
                                            onClick = {
                                                viewModel.triggerEsp32IncomingSos(device.deviceId, "MANUAL_BUTTON_SOS")
                                            },
                                            shape = RoundedCornerShape(8.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.onErrorContainer, contentColor = MaterialTheme.colorScheme.errorContainer),
                                            modifier = Modifier.height(36.dp).weight(1f)
                                        ) {
                                            Text("Press SOS Button 🚨", fontSize = 11.sp)
                                        }

                                        Button(
                                            onClick = {
                                                viewModel.triggerManualHeartbeatCheck(device.deviceId)
                                            },
                                            shape = RoundedCornerShape(8.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer),
                                            modifier = Modifier.height(36.dp).weight(1f)
                                        ) {
                                            Text("Ping / Heartbeat 💓", fontSize = 11.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Handshake and Device Registration controller (for pairing new nodes with token/authentication checks)
                    item {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
                        ) {
                            var regName by remember { mutableStateOf("Safety Wearable ESP32") }
                            var regMac by remember { mutableStateOf("30:AE:A4:07:0D:64") }
                            var regToken by remember { mutableStateOf("secure-handshake-token") }
                            var regFirmware by remember { mutableStateOf("v1.2.8-esp32") }

                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = "Pair New ESP32 Node via Authenticated Handshake",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Registers node onto secure telemetry mesh after token verification & firmware capability checks.",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                OutlinedTextField(
                                    value = regName,
                                    onValueChange = { regName = it },
                                    label = { Text("Device Display Name", fontSize = 11.sp) },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth().height(52.dp)
                                )

                                OutlinedTextField(
                                    value = regMac,
                                    onValueChange = { regMac = it },
                                    label = { Text("Device MAC Address", fontSize = 11.sp) },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth().height(52.dp)
                                )

                                OutlinedTextField(
                                    value = regToken,
                                    onValueChange = { regToken = it },
                                    label = { Text("Authentication Security Token", fontSize = 11.sp) },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth().height(52.dp)
                                )

                                OutlinedTextField(
                                    value = regFirmware,
                                    onValueChange = { regFirmware = it },
                                    label = { Text("Firmware Version (e.g. v1.2.8 or v1.1.0)", fontSize = 11.sp) },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth().height(52.dp)
                                )

                                Button(
                                    onClick = {
                                        viewModel.authenticateAndRegisterESP32(
                                            name = regName,
                                            mac = regMac,
                                            token = regToken,
                                            firmware = regFirmware
                                        ) { res ->
                                            if (res.isSuccess) {
                                                selectedDeviceId = res.getOrThrow().deviceId
                                            }
                                        }
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(40.dp)
                                ) {
                                    Text("Authenticate & Pair Node 🔑", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
            }
        }
    }

    // Rename dialog
    if (showRenameDialog) {
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text("Rename Monitored Node") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Specify a descriptive nickname for this guardian sensor beacon node.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = renameDeviceName,
                        onValueChange = { renameDeviceName = it },
                        label = { Text("Device Nickname") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("device_rename_text_field")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (renameDeviceName.isNotBlank() && selectedDeviceId.isNotBlank()) {
                            viewModel.renameDevice(selectedDeviceId, renameDeviceName.trim())
                            showRenameDialog = false
                        }
                    },
                    modifier = Modifier.testTag("device_rename_confirm_button")
                ) {
                    Text("Rename")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun IndicatorBadge(
    label: String,
    value: String,
    success: Boolean
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .background(
                    color = if (success) SafetyGreen else MaterialTheme.colorScheme.error,
                    shape = CircleShape
                )
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = "$label: $value",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun TelemetryMetricCard(
    title: String,
    value: String,
    subValue: String,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = modifier.border(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f),
            shape = RoundedCornerShape(16.dp)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold
                )
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .background(accentColor.copy(alpha = 0.12f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = value,
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subValue,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
fun TelemetryGaugeCard(
    title: String,
    percent: Int,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = modifier.border(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f),
            shape = RoundedCornerShape(16.dp)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold
                )
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .background(accentColor.copy(alpha = 0.12f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "$percent%",
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Linear Progress Bar
            LinearProgressIndicator(
                progress = percent / 100f,
                color = accentColor,
                trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f),
                strokeCap = StrokeCap.Round,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
            )
        }
    }
}
