package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Device
import com.example.ui.GuardianViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DevicePairingScreen(
    viewModel: GuardianViewModel,
    onNavigateBack: () -> Unit
) {
    val devices by viewModel.devices.collectAsState()
    val scope = rememberCoroutineScope()

    // Screen tabs: "My Devices" & "Pair New"
    var activeTab by remember { mutableStateOf(0) }

    // Dialog & Simulation states
    var showRenameDialog by remember { mutableStateOf<Device?>(null) }
    var showUnpairConfirm by remember { mutableStateOf<Device?>(null) }
    var showPairingWizard by remember { mutableStateOf<String?>(null) } // "BT", "QR", "WIFI", "ID"

    // Pairing fields state
    var manualId by remember { mutableStateOf("") }
    var manualName by remember { mutableStateOf("") }
    var manualMac by remember { mutableStateOf("") }

    var wifiSsid by remember { mutableStateOf("") }
    var wifiPass by remember { mutableStateOf("") }

    // Simulated scanning progress
    var isSimulatingPairing by remember { mutableStateOf(false) }
    var simulationProgress by remember { mutableStateOf(0f) }
    var simulatedStatusMessage by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "ESP32 Device Bonding",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("device_pairing_back")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Navigate Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // --- TOP TAB ROW ---
            TabRow(
                selectedTabIndex = activeTab,
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                contentColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.fillMaxWidth()
            ) {
                Tab(
                    selected = activeTab == 0,
                    onClick = { activeTab = 0 },
                    text = { Text("My Paired Devices", fontWeight = FontWeight.Bold) },
                    icon = { Icon(Icons.Default.DeveloperBoard, contentDescription = null) },
                    modifier = Modifier.testTag("tab_my_devices")
                )
                Tab(
                    selected = activeTab == 1,
                    onClick = { activeTab = 1 },
                    text = { Text("Pair New ESP32", fontWeight = FontWeight.Bold) },
                    icon = { Icon(Icons.Default.AddCircleOutline, contentDescription = null) },
                    modifier = Modifier.testTag("tab_pair_new")
                )
            }

            if (activeTab == 0) {
                // --- MY PAIRED DEVICES ---
                if (devices.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("🛰️", fontSize = 36.sp)
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No Connected Wearables",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Secure your health by pairing an ESP32 SOS wearable band via Bluetooth, WiFi, or QR-Code.",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = { activeTab = 1 },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Pair Your First Device")
                        }
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .testTag("devices_list")
                    ) {
                        items(devices, key = { it.deviceId }) { device ->
                            DeviceCardItem(
                                device = device,
                                onRename = { showRenameDialog = device },
                                onUnpair = { showUnpairConfirm = device }
                            )
                        }
                    }
                }
            } else {
                // --- PAIR NEW ESP32 WIZARD MENU ---
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(16.dp)
                        .background(MaterialTheme.colorScheme.background)
                ) {
                    Text(
                        text = "Choose Connection Mode",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 12.dp, start = 4.dp)
                    )

                    // BLE Option
                    PairingMethodCard(
                        title = "Simulate Bluetooth Low Energy (BLE)",
                        description = "Scan for nearby broadcasting Guardian ESP32 wristbands automatically.",
                        icon = "📶",
                        tag = "pair_ble_card",
                        onClick = {
                            showPairingWizard = "BT"
                            isSimulatingPairing = true
                            simulationProgress = 0f
                            simulatedStatusMessage = "Initializing Android BLE Radio..."
                            scope.launch {
                                delay(600)
                                simulatedStatusMessage = "Scanning channels (2.4GHz)..."
                                while (simulationProgress < 1.0f) {
                                    delay(400)
                                    simulationProgress += 0.2f
                                    if (simulationProgress >= 0.4f && simulationProgress < 0.8f) {
                                        simulatedStatusMessage = "Found: Guardian Band BLE [30:AE:A4:07:0D:64] RSSI -59dB"
                                    } else if (simulationProgress >= 0.8f) {
                                        simulatedStatusMessage = "Bonding and establishing encrypted BLE profile..."
                                    }
                                }
                                viewModel.bondDevice(
                                    name = "Guardian Wristband ESP32",
                                    mac = "30:AE:A4:07:0D:64",
                                    firmware = "v1.2.8-esp32-ble",
                                    battery = 92,
                                    signal = -59,
                                    health = "EXCELLENT"
                                )
                                isSimulatingPairing = false
                                showPairingWizard = null
                                activeTab = 0
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // QR Option
                    PairingMethodCard(
                        title = "Scan QR Code",
                        description = "Point the camera at the QR code printed on the ESP32 housing or user manual.",
                        icon = "📷",
                        tag = "pair_qr_card",
                        onClick = {
                            showPairingWizard = "QR"
                            isSimulatingPairing = true
                            simulationProgress = 0f
                            simulatedStatusMessage = "Requesting Camera Frame Permissions..."
                            scope.launch {
                                delay(800)
                                simulatedStatusMessage = "Targeting QR bounds. Searching for ESP32 QR signature..."
                                delay(1200)
                                simulatedStatusMessage = "Parsed: ESP32_G_81F4 Mac: 24:0A:C4:81:8A:F4"
                                delay(1000)
                                viewModel.bondDevice(
                                    name = "Guardian Smart-Band QR",
                                    mac = "24:0A:C4:81:8A:F4",
                                    firmware = "v1.3.1-esp32",
                                    battery = 88,
                                    signal = -64,
                                    health = "EXCELLENT"
                                )
                                isSimulatingPairing = false
                                showPairingWizard = null
                                activeTab = 0
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // WiFi Option
                    PairingMethodCard(
                        title = "Smart WiFi Provisioning",
                        description = "Provision WiFi credentials directly to ESP32 Smart-Config access points.",
                        icon = "🌐",
                        tag = "pair_wifi_card",
                        onClick = {
                            showPairingWizard = "WIFI"
                            wifiSsid = "Home_Secure_WiFi"
                            wifiPass = ""
                        }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Manual ID Option
                    PairingMethodCard(
                        title = "Enter Device ID Manually",
                        description = "Manually key in the serial alphanumeric Device ID & MAC signature.",
                        icon = "✍️",
                        tag = "pair_id_card",
                        onClick = {
                            showPairingWizard = "ID"
                            manualId = "esp32-" + UUID.randomUUID().toString().take(6).uppercase()
                            manualName = "Custom Guardian Band"
                            manualMac = "A4:CF:12:34:56:AB"
                        }
                    )
                }
            }
        }

        // --- PAIRING WIZARD SIMULATION DIALOG ---
        if (showPairingWizard != null && isSimulatingPairing) {
            AlertDialog(
                onDismissRequest = { /* Prevent cancellation during simulation */ },
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                        Text("Connecting Wearable", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = simulatedStatusMessage,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                        LinearProgressIndicator(
                            progress = { simulationProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                        )
                        Text(
                            text = "Please keep your wearable powered on and within 3 meters.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                },
                confirmButton = {}
            )
        }

        // --- WIFI PROVISIONING DIALOG ---
        if (showPairingWizard == "WIFI" && !isSimulatingPairing) {
            AlertDialog(
                onDismissRequest = { showPairingWizard = null },
                title = { Text("Smart WiFi Provisioning", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = "Provide details to transmit to your ESP32's Smart-Config AP.",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        OutlinedTextField(
                            value = wifiSsid,
                            onValueChange = { wifiSsid = it },
                            label = { Text("WiFi Network Name (SSID)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = wifiPass,
                            onValueChange = { wifiPass = it },
                            label = { Text("Network Password") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            isSimulatingPairing = true
                            simulationProgress = 0f
                            simulatedStatusMessage = "Broadcasting UDP Smart-Config beacon..."
                            scope.launch {
                                delay(1000)
                                simulatedStatusMessage = "ESP32 caught broadcast! Authenticating with router..."
                                delay(1200)
                                simulatedStatusMessage = "ESP32 assigned IP 192.168.1.144. Syncing token..."
                                delay(1000)
                                viewModel.bondDevice(
                                    name = "Guardian Smart-Band WiFi",
                                    mac = "24:0A:C4:58:22:1A",
                                    firmware = "v1.2.9-esp32-wifi",
                                    battery = 99,
                                    signal = -48,
                                    health = "EXCELLENT"
                                )
                                isSimulatingPairing = false
                                showPairingWizard = null
                                activeTab = 0
                            }
                        }
                    ) {
                        Text("Broadcast Credentials")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showPairingWizard = null }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // --- MANUAL ID PAIRING DIALOG ---
        if (showPairingWizard == "ID" && !isSimulatingPairing) {
            AlertDialog(
                onDismissRequest = { showPairingWizard = null },
                title = { Text("Pair Using Device ID", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = manualName,
                            onValueChange = { manualName = it },
                            label = { Text("Custom Device Name") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("pairing_manual_name")
                        )
                        OutlinedTextField(
                            value = manualId,
                            onValueChange = { manualId = it },
                            label = { Text("Device ID Signature") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("pairing_manual_id")
                        )
                        OutlinedTextField(
                            value = manualMac,
                            onValueChange = { manualMac = it },
                            label = { Text("Hardware MAC Address") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("pairing_manual_mac")
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.bondDevice(
                                name = manualName.trim().ifEmpty { "Manual ESP32 Band" },
                                mac = manualMac.trim().ifEmpty { "00:11:22:33:44:55" },
                                deviceId = manualId.trim().ifEmpty { "esp32-manual" },
                                firmware = "v1.0.0-manual",
                                battery = 100,
                                signal = -70,
                                health = "GOOD"
                            )
                            showPairingWizard = null
                            activeTab = 0
                        },
                        modifier = Modifier.testTag("pairing_confirm_btn")
                    ) {
                        Text("Register Device")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showPairingWizard = null }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // --- RENAME DEVICE DIALOG ---
        if (showRenameDialog != null) {
            var newName by remember { mutableStateOf(showRenameDialog?.deviceName ?: "") }
            AlertDialog(
                onDismissRequest = { showRenameDialog = null },
                title = { Text("Rename ESP32 Device", fontWeight = FontWeight.Bold) },
                text = {
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        label = { Text("Device Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("rename_device_input")
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showRenameDialog?.deviceId?.let { id ->
                                viewModel.renameDevice(id, newName.trim())
                            }
                            showRenameDialog = null
                        },
                        modifier = Modifier.testTag("rename_device_confirm_btn")
                    ) {
                        Text("Save")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showRenameDialog = null }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // --- UNPAIR DEVICE CONFIRMATION ---
        if (showUnpairConfirm != null) {
            AlertDialog(
                onDismissRequest = { showUnpairConfirm = null },
                title = { Text("Unpair Emergency Wearable?", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error) },
                text = {
                    Text(
                        "Are you absolutely sure you want to unpair ${showUnpairConfirm?.deviceName}? This will immediately sever emergency fall detection signals and panic-button triggers."
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showUnpairConfirm?.deviceId?.let { id ->
                                viewModel.unbondDevice(id)
                            }
                            showUnpairConfirm = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.testTag("unpair_device_confirm_btn")
                    ) {
                        Text("Unpair & sever")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showUnpairConfirm = null }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
fun PairingMethodCard(
    title: String,
    description: String,
    icon: String,
    tag: String,
    onClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFFF1F3F7), RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .testTag(tag)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(text = icon, fontSize = 22.sp)
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = description,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 16.sp
                )
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline
            )
        }
    }
}

@Composable
fun DeviceCardItem(
    device: Device,
    onRename: () -> Unit,
    onUnpair: () -> Unit
) {
    val formatter = remember { SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()) }
    val lastSeenStr = formatter.format(Date(device.lastSync))

    // Color code health status
    val (healthText, healthColor) = when (device.deviceHealth.uppercase()) {
        "EXCELLENT" -> Pair("EXCELLENT", Color(0xFF4CAF50))
        "GOOD" -> Pair("GOOD", Color(0xFF8BC34A))
        "WARNING" -> Pair("WARNING", Color(0xFFFF9800))
        else -> Pair("CRITICAL", Color(0xFFF44336))
    }

    // Color code connection status
    val statusColor = if (device.status == "CONNECTED") Color(0xFF4CAF50) else Color(0xFFF44336)

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFFE2E7F0), RoundedCornerShape(24.dp))
            .testTag("paired_device_card_${device.deviceId}")
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header Gradient Strip
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.secondary
                            )
                        )
                    )
            )

            Column(modifier = Modifier.padding(18.dp)) {
                // Device Icon + Core Identity
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("📟", fontSize = 28.sp)
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = device.deviceName,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.testTag("device_item_name_${device.deviceId}")
                            )
                            IconButton(
                                onClick = onRename,
                                modifier = Modifier
                                    .size(24.dp)
                                    .testTag("device_rename_btn_${device.deviceId}")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Rename device",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                        
                        Text(
                            text = "MAC: ${device.macAddress}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    // Connection status badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(statusColor.copy(alpha = 0.1f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(statusColor)
                            )
                            Text(
                                text = device.status,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = statusColor
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = Color(0xFFF1F3F7))
                Spacer(modifier = Modifier.height(12.dp))

                // Telemetry Details Grid
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Battery
                    TelemetryField(
                        label = "Battery",
                        value = "${device.batteryLevel}%",
                        icon = {
                            Icon(
                                imageVector = when {
                                    device.batteryLevel > 80 -> Icons.Default.BatteryFull
                                    device.batteryLevel > 30 -> Icons.Default.BatteryChargingFull
                                    else -> Icons.Default.BatteryAlert
                                },
                                contentDescription = null,
                                tint = if (device.batteryLevel > 30) Color(0xFF4CAF50) else Color(0xFFF44336),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    )

                    // Signal
                    TelemetryField(
                        label = "Signal (RSSI)",
                        value = "${device.signalStrength} dBm",
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Wifi,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    )

                    // Health
                    TelemetryField(
                        label = "Device Health",
                        value = healthText,
                        icon = {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = healthColor,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = Color(0xFFF1F3F7))
                Spacer(modifier = Modifier.height(12.dp))

                // Firmware version & Last seen footer
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Firmware: ${device.firmwareVersion}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Last Synced: $lastSeenStr",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Unpair Button
                    TextButton(
                        onClick = onUnpair,
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.testTag("unpair_btn_${device.deviceId}")
                    ) {
                        Icon(Icons.Default.LinkOff, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Unpair", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun TelemetryField(
    label: String,
    value: String,
    icon: @Composable () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(text = label, fontSize = 11.sp, color = Color(0xFF7A8B9E), fontWeight = FontWeight.SemiBold)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            icon()
            Text(
                text = value,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
