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
    val authState by viewModel.authState.collectAsState()
    val alerts by viewModel.alerts.collectAsState()
    val devices by viewModel.devices.collectAsState()
    val emergencySession by viewModel.emergencySession.collectAsState()
    val notifications by viewModel.notifications.collectAsState()
    val currentUser = (authState as? AuthState.Success)?.user ?: User(name = "User")

    var showBondDialog by remember { mutableStateOf(false) }
    var showResolveDialog by remember { mutableStateOf<Alert?>(null) }

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
                    viewModel.triggerManualSOS()
                    onNavigateToEmergency()
                })
            }

            // Quick Status Grid
            item {
                StatusGrid(devices = devices)
            }

            // Active Alerts
            if (alerts.any { it.status == "ACTIVE" }) {
                item {
                    Text("Active Emergencies", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                items(alerts.filter { it.status == "ACTIVE" }) { alert ->
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
                items(devices) { device ->
                    DeviceCard(
                        device = device,
                        onSimulateClick = { reason -> viewModel.triggerEsp32SOS(reason) },
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
fun StatusGrid(devices: List<Device>) {
    val isBleConnected = devices.any { it.status == "CONNECTED" }
    val maxBattery = devices.maxOfOrNull { it.batteryLevel } ?: 0
    
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
            value = if (devices.isNotEmpty()) "$maxBattery%" else "--",
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
fun DeviceCard(device: Device, onSimulateClick: (String) -> Unit, onMonitorClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(40.dp).background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Watch, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(device.deviceName, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text(device.macAddress, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Badge(containerColor = if (device.status == "CONNECTED") SafetyGreen else AlertOrange) {
                    Text(if (device.status == "CONNECTED") "Online" else "Offline", modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { onSimulateClick("FALL_DETECTED") }, modifier = Modifier.weight(1f)) {
                    Text("Simulate Fall", fontSize = 11.sp)
                }
                Button(onClick = onMonitorClick, modifier = Modifier.weight(1f)) {
                    Text("Diagnostics", fontSize = 11.sp)
                }
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
