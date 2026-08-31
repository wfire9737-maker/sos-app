package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.ui.rememberLocationPermissionHandler
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import com.example.ui.GuardianViewModel
import com.example.service.AuthState
import com.example.model.*
import com.example.ui.theme.*

@Composable
fun HomeScreen(
    viewModel: GuardianViewModel,
    onNavigateToLogin: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToContacts: () -> Unit,
    onNavigateToDevicePairing: () -> Unit,
    onNavigateToMap: () -> Unit = {},
    onNavigateToBleTest: () -> Unit = {},
    onNavigateToNearbyDiscovery: () -> Unit = {},
    onNavigateToEmergency: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToAiDashboard: () -> Unit,
    onNavigateToDeviceMonitoring: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToAnalytics: () -> Unit,
    onNavigateToReports: () -> Unit,
    onNavigateToSafeCheckIn: () -> Unit = {}
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val sosTriggerHandler = rememberLocationPermissionHandler {
        viewModel.triggerManualSOS()
        onNavigateToEmergency()
    }
    
    val authState by viewModel.authState.collectAsState()
    val alerts by viewModel.alerts.collectAsState()
    val devices by viewModel.devices.collectAsState()
    val isEsp32Connected by viewModel.isEsp32Connected.collectAsState()
    val emergencySession by viewModel.emergencySession.collectAsState()
    val notifications by viewModel.notifications.collectAsState()
    val currentUser = (authState as? AuthState.Success)?.user ?: User(name = "User")
    val sosWorkflowState by viewModel.sosWorkflowState.collectAsState()

    var showBondDialog by remember { mutableStateOf(false) }
    var showResolveDialog by remember { mutableStateOf<Alert?>(null) }
    
    if (sosWorkflowState != SosWorkflowState.IDLE) {
        androidx.compose.ui.window.Dialog(onDismissRequest = {}) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier.padding(24.dp).fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = when (sosWorkflowState) {
                            SosWorkflowState.OBTAINING_LOCATION -> "Obtaining high accuracy location..."
                            SosWorkflowState.SENDING_SMS -> "Sending SMS to emergency contacts..."
                            SosWorkflowState.CALLING_CONTACT -> "Placing emergency call..."
                            SosWorkflowState.UPLOADING -> "Uploading SOS alert to servers..."
                            SosWorkflowState.COMPLETED -> "SOS Completed!"
                            else -> "Preparing SOS..."
                        },
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }

    Scaffold(
        bottomBar = {
            HomeBottomNav(
                onNavigateToMap = onNavigateToMap,
                onNavigateToContacts = onNavigateToContacts,
                onNavigateToHistory = onNavigateToHistory,
                onNavigateToSettings = onNavigateToSettings
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(24.dp))
                HomeHeader(
                    userName = currentUser.name,
                    onProfileClick = onNavigateToProfile,
                    onNotificationsClick = onNavigateToNotifications,
                    unreadCount = notifications.count { !it.isRead }
                )
            }

            // Big SOS Button
            item {
                Spacer(modifier = Modifier.height(16.dp))
                SosButtonSection(onSosClick = {
                    sosTriggerHandler()
                })
            }

            // Voice Command & Speech Recognition Section
            item {
                VoiceCommandSection(viewModel = viewModel)
            }

            // Quick Status Grid
            item {
                StatusGrid(devices = devices, onBluetoothClick = onNavigateToBleTest)
            }
            
            // Nearby Discovery Section
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().clickable { onNavigateToNearbyDiscovery() },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(MaterialTheme.colorScheme.primary, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Person, contentDescription = "Nearby People", tint = MaterialTheme.colorScheme.onPrimary)
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text("Nearby People", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                            Text("Find and connect with nearby users", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f))
                        }
                    }
                }
            }

            // Active Alerts
            if (alerts.any { it.status == "ACTIVE" }) {
                item {
                    Text("Active Emergencies", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                items(alerts.filter { it.status == "ACTIVE" }, key = { it.id }) { alert ->
                    AlertCard(alert = alert, onResolveClick = { showResolveDialog = alert })
                }
            }

            // Paired Devices
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Connected Devices", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    TextButton(onClick = { showBondDialog = true }) {
                        Text("+ ADD", color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
            if (devices.isEmpty()) {
                item {
                    Text("No devices bonded.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                }
            } else {
                items(devices, key = { it.deviceId }) { device ->
                    DeviceCard(
                        device = device,
                        isEsp32Connected = isEsp32Connected,
                        onMonitorClick = onNavigateToDeviceMonitoring
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }

    if (showBondDialog) {
        BondDeviceDialog(
            onDismiss = { showBondDialog = false },
            onBondConfirm = { name, mac ->
                viewModel.bondDevice(name, mac)
                showBondDialog = false
            }
        )
    }

    showResolveDialog?.let { alert ->
        ResolveAlertDialog(
            alert = alert,
            onDismiss = { showResolveDialog = null },
            onResolveConfirm = { notes ->
                viewModel.resolveAlert(alert.id, notes)
                showResolveDialog = null
            }
        )
    }
}

@Composable
fun HomeHeader(userName: String, onProfileClick: () -> Unit, onNotificationsClick: () -> Unit, unreadCount: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text("Stay Safe,", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(userName, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(contentAlignment = Alignment.Center) {
                IconButton(onClick = onNotificationsClick) {
                    Icon(Icons.Default.Notifications, contentDescription = "Notifications", tint = MaterialTheme.colorScheme.onSurface)
                }
                if (unreadCount > 0) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(4.dp)
                            .size(10.dp)
                            .background(MaterialTheme.colorScheme.error, CircleShape)
                    )
                }
            }
            IconButton(onClick = onProfileClick, modifier = Modifier.background(MaterialTheme.colorScheme.primaryContainer, CircleShape)) {
                Icon(Icons.Default.Person, contentDescription = "Profile", tint = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
fun SosButtonSection(onSosClick: () -> Unit) {
    var isPressed by remember { mutableStateOf(false) }
    
    Box(
        modifier = Modifier.fillMaxWidth().height(260.dp),
        contentAlignment = Alignment.Center
    ) {
        // Outer rings
        Box(modifier = Modifier.size(240.dp).background(MaterialTheme.colorScheme.error.copy(alpha = 0.1f), CircleShape))
        Box(modifier = Modifier.size(190.dp).background(MaterialTheme.colorScheme.error.copy(alpha = 0.2f), CircleShape))
        
        // Inner button
        Box(
            modifier = Modifier
                .size(140.dp)
                .background(MaterialTheme.colorScheme.error, CircleShape)
                .clip(CircleShape)
                .clickable { onSosClick() },
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("SOS", color = MaterialTheme.colorScheme.onError, fontSize = 36.sp, fontWeight = FontWeight.Black)
                Text("TAP FOR HELP", color = MaterialTheme.colorScheme.onError.copy(alpha = 0.8f), fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun StatusGrid(devices: List<Device>, onBluetoothClick: () -> Unit = {}) {
    val isBleConnected = devices.any { it.status == "CONNECTED" || it.status == "ALERTing" }
    val maxBattery = devices.filter { it.status == "CONNECTED" || it.status == "ALERTing" }.maxOfOrNull { it.batteryLevel } ?: 0
    
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        StatusCard(
            modifier = Modifier.weight(1f).clickable { onBluetoothClick() },
            icon = Icons.Default.Bluetooth,
            label = "Bluetooth",
            value = if (isBleConnected) "Connected" else "Disconnected",
            statusColor = if (isBleConnected) SafetyGreen else MaterialTheme.colorScheme.onSurfaceVariant
        )
        StatusCard(
            modifier = Modifier.weight(1f),
            icon = Icons.Default.BatteryFull,
            label = "Battery",
            value = if (isBleConnected) "$maxBattery%" else "--",
            statusColor = if (maxBattery > 20) SafetyGreen else if (maxBattery > 0) AlertOrange else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun StatusCard(modifier: Modifier = Modifier, icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String, statusColor: Color) {
    Card(
        modifier = modifier.height(90.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp).fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(icon, contentDescription = null, tint = statusColor, modifier = Modifier.size(24.dp))
            Column {
                Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(value, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}

@Composable
fun DeviceCard(device: Device, isEsp32Connected: Boolean, onMonitorClick: () -> Unit) {
    val isConnected = isEsp32Connected || device.status == "CONNECTED"
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(40.dp).background(if (isConnected) SafetyGreen.copy(alpha = 0.2f) else MaterialTheme.colorScheme.errorContainer, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Watch, contentDescription = null, tint = if (isConnected) SafetyGreen else MaterialTheme.colorScheme.error)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(device.deviceName, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text(device.macAddress, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Badge(containerColor = if (isConnected) SafetyGreen else MaterialTheme.colorScheme.error) {
                    Text(
                        if (isConnected) "Connected to SOS Device" else "SOS Device Disconnected",
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onMonitorClick, modifier = Modifier.fillMaxWidth()) {
                Text("Diagnostics", fontSize = 11.sp)
            }
        }
    }
}

@Composable
fun AlertCard(alert: Alert, onResolveClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                Spacer(modifier = Modifier.width(8.dp))
                Text("SOS Triggered", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onErrorContainer)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text("Reason: ${alert.triggerType}", fontSize = 14.sp, color = MaterialTheme.colorScheme.onErrorContainer)
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = onResolveClick,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Resolve Alert")
            }
        }
    }
}

@Composable
fun BondDeviceDialog(onDismiss: () -> Unit, onBondConfirm: (String, String) -> Unit) {
    var nickname by remember { mutableStateOf("") }
    var macAddress by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Pair Wearable", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(value = nickname, onValueChange = { nickname = it }, label = { Text("Device Name") })
                OutlinedTextField(value = macAddress, onValueChange = { macAddress = it }, label = { Text("MAC Address") })
            }
        },
        confirmButton = {
            Button(onClick = { onBondConfirm(nickname, macAddress) }) { Text("Pair") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun ResolveAlertDialog(alert: Alert, onDismiss: () -> Unit, onResolveConfirm: (String) -> Unit) {
    var notes by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Resolve Emergency") },
        text = {
            OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Notes") })
        },
        confirmButton = {
            Button(onClick = { onResolveConfirm(notes) }) { Text("Resolve") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun HomeBottomNav(onNavigateToMap: () -> Unit, onNavigateToContacts: () -> Unit, onNavigateToHistory: () -> Unit, onNavigateToSettings: () -> Unit) {
    NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
        NavigationBarItem(
            icon = { Icon(Icons.Default.Home, contentDescription = null) },
            label = { Text("Home") },
            selected = true,
            onClick = { }
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.Map, contentDescription = null) },
            label = { Text("Map") },
            selected = false,
            onClick = onNavigateToMap
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.Contacts, contentDescription = null) },
            label = { Text("Contacts") },
            selected = false,
            onClick = onNavigateToContacts
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.History, contentDescription = null) },
            label = { Text("History") },
            selected = false,
            onClick = onNavigateToHistory
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.Settings, contentDescription = null) },
            label = { Text("Settings") },
            selected = false,
            onClick = onNavigateToSettings
        )
    }
}

@Composable
fun VoiceCommandSection(
    viewModel: GuardianViewModel,
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val isSpeechActive by viewModel.isSpeechRecognizerActive.collectAsState()
    val liveSpokenText by viewModel.liveSpokenText.collectAsState()
    val speechStatus by viewModel.speechStatusMessage.collectAsState()
    val micDecibels by viewModel.micDecibels.collectAsState()
    val confirmationMsg by viewModel.voiceCommandConfirmation.collectAsState()

    val micPermissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.startVoiceRecognition(context)
        } else {
            android.widget.Toast.makeText(context, "Microphone permission required for voice commands", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    fun toggleMic() {
        val hasPermission = androidx.core.content.ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.RECORD_AUDIO
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED

        if (isSpeechActive) {
            viewModel.stopVoiceRecognition()
        } else {
            if (hasPermission) {
                viewModel.startVoiceRecognition(context)
            } else {
                micPermissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
            }
        }
    }

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Confirmation Banner
        AnimatedVisibility(
            visible = confirmationMsg != null,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            confirmationMsg?.let { msg ->
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (msg.contains("Triggered")) MaterialTheme.colorScheme.errorContainer
                        else if (msg.contains("cancelled")) MaterialTheme.colorScheme.secondaryContainer
                        else MaterialTheme.colorScheme.primaryContainer
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().testTag("voice_confirmation_banner")
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp).fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (msg.contains("Triggered")) Icons.Default.Warning
                                else if (msg.contains("cancelled")) Icons.Default.CheckCircle
                                else Icons.Default.MyLocation,
                                contentDescription = null,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = msg,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        IconButton(onClick = { viewModel.clearVoiceCommandConfirmation() }) {
                            Icon(Icons.Default.Close, contentDescription = "Dismiss")
                        }
                    }
                }
            }
        }

        // Voice Control Card
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { toggleMic() },
                            modifier = Modifier
                                .size(52.dp)
                                .background(
                                    if (isSpeechActive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                                    CircleShape
                                )
                                .testTag("mic_button")
                        ) {
                            Icon(
                                imageVector = if (isSpeechActive) Icons.Default.Mic else Icons.Default.MicNone,
                                contentDescription = "Microphone",
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = if (isSpeechActive) "Listening for Command..." else "Voice Recognition",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = speechStatus,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (isSpeechActive) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.errorContainer,
                            modifier = Modifier.padding(4.dp)
                        ) {
                            Text(
                                text = "${micDecibels.toInt()} dB",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }

                // Live Spoken Text Display
                AnimatedVisibility(visible = liveSpokenText.isNotBlank()) {
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.RecordVoiceOver,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "\"$liveSpokenText\"",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}
