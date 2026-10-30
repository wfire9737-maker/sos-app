package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.ui.GuardianViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NearbyDiscoveryScreen(
    viewModel: GuardianViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToChat: (String, String) -> Unit = { _, _ -> },
    onNavigateToLocations: () -> Unit = {}
) {
    val context = LocalContext.current
    val nearbyDevices by viewModel.nearbyBleManager.nearbyDevices.collectAsState()

    val permissions = remember {
        mutableListOf<String>().apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                add(Manifest.permission.BLUETOOTH_SCAN)
                add(Manifest.permission.BLUETOOTH_CONNECT)
                add(Manifest.permission.BLUETOOTH_ADVERTISE)
            } else {
                add(Manifest.permission.ACCESS_FINE_LOCATION)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    var hasPermissions by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        hasPermissions = results.values.all { it }
        if (hasPermissions) {
            Log.d("NearbyDiscoveryScreen", "NEARBY_DEBUG: Discovery screen entered")
            viewModel.nearbyBleManager.startScanningForNearby()
            Log.d("NearbyDiscoveryScreen", "NEARBY_DEBUG: Scanner started")
            viewModel.nearbyBleManager.startAdvertisingPresence()
            Log.d("NearbyDiscoveryScreen", "NEARBY_DEBUG: Foreground presence started")
        }
    }

    DisposableEffect(Unit) {
        val allGranted = permissions.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
        if (allGranted) {
            hasPermissions = true
            Log.d("NearbyDiscoveryScreen", "NEARBY_DEBUG: Discovery screen entered")
            viewModel.nearbyBleManager.startScanningForNearby()
            Log.d("NearbyDiscoveryScreen", "NEARBY_DEBUG: Scanner started")
            viewModel.nearbyBleManager.startAdvertisingPresence()
            Log.d("NearbyDiscoveryScreen", "NEARBY_DEBUG: Foreground presence started")
        } else {
            permissionLauncher.launch(permissions.toTypedArray())
        }

        onDispose {
            Log.d("NearbyDiscoveryScreen", "NEARBY_DEBUG: Discovery screen leaving")
            viewModel.nearbyBleManager.stopAdvertisingPresence()
            Log.d("NearbyDiscoveryScreen", "NEARBY_DEBUG: Foreground presence stopped")
            viewModel.nearbyBleManager.stopScanningForNearby()
        }
    }

    var isRefreshing by remember { mutableStateOf(false) }

    val sortedDevices = remember(nearbyDevices) {
        val (connected, notConnected) = nearbyDevices.values.partition {
            it.connectionState == com.example.ble.nearby.NearbyConnectionState.CONNECTED
        }
        val (requesting, disconnected) = notConnected.partition {
            it.connectionState == com.example.ble.nearby.NearbyConnectionState.REQUESTING
        }
        // 1. Pinned connected users at the top in stable order
        // 2. Connecting/requesting users
        // 3. Available users sorted by last seen
        connected.sortedBy { it.deviceName.lowercase() } +
        requesting.sortedByDescending { it.lastSeen } +
        disconnected.sortedByDescending { it.lastSeen }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Nearby People") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (isRefreshing) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .padding(horizontal = 12.dp)
                                .size(20.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        IconButton(
                            onClick = {
                                if (!isRefreshing) {
                                    isRefreshing = true
                                    viewModel.nearbyBleManager.manualRefresh {
                                        isRefreshing = false
                                    }
                                }
                            },
                            modifier = Modifier.testTag("nearby_refresh_button")
                        ) {
                            Icon(
                                Icons.Default.Refresh,
                                contentDescription = "Refresh nearby devices",
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                    IconButton(onClick = onNavigateToLocations) {
                        Icon(Icons.Default.LocationOn, contentDescription = "View Shared Locations", tint = MaterialTheme.colorScheme.onPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            if (!hasPermissions) {
                Text(
                    text = "Bluetooth scanning permissions are required to discover nearby users.",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            } else {
                // Header with Discovered Users count and visible Refresh Button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Discovered Users",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        val connectedCount = sortedDevices.count { it.connectionState == com.example.ble.nearby.NearbyConnectionState.CONNECTED }
                        Text(
                            text = if (connectedCount > 0) "$connectedCount Connected • ${sortedDevices.size} Total" else "${sortedDevices.size} Found",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    OutlinedButton(
                        onClick = {
                            if (!isRefreshing) {
                                isRefreshing = true
                                viewModel.nearbyBleManager.manualRefresh {
                                    isRefreshing = false
                                }
                            }
                        },
                        enabled = !isRefreshing,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("nearby_manual_refresh_action")
                    ) {
                        if (isRefreshing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(14.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Scanning...", style = MaterialTheme.typography.bodySmall)
                        } else {
                            Icon(
                                Icons.Default.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Refresh", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }

                if (sortedDevices.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "No nearby users detected.",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Button(
                                onClick = {
                                    if (!isRefreshing) {
                                        isRefreshing = true
                                        viewModel.nearbyBleManager.manualRefresh {
                                            isRefreshing = false
                                        }
                                    }
                                },
                                enabled = !isRefreshing
                            ) {
                                if (isRefreshing) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Searching...")
                                } else {
                                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Search Nearby")
                                }
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(
                            items = sortedDevices,
                            key = { it.id }
                        ) { device ->
                            val isConnected = device.connectionState == com.example.ble.nearby.NearbyConnectionState.CONNECTED
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("nearby_device_card_${device.id}"),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isConnected) {
                                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                                    } else {
                                        MaterialTheme.colorScheme.surfaceVariant
                                    }
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = "Nearby User",
                                        tint = if (isConnected) Color(0xFF2E7D32) else MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(40.dp)
                                    )
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        if (isConnected) {
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = Color(0xFFE8F5E9),
                                                modifier = Modifier.padding(bottom = 2.dp)
                                            ) {
                                                Text(
                                                    text = "PINNED • CONNECTED",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF2E7D32),
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                        Text(
                                            text = device.deviceName.ifBlank { "Smart SOS Phone" },
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = when (device.connectionState) {
                                                com.example.ble.nearby.NearbyConnectionState.CONNECTED -> "Connected"
                                                com.example.ble.nearby.NearbyConnectionState.REQUESTING -> "Connecting..."
                                                com.example.ble.nearby.NearbyConnectionState.DISCONNECTED -> "Available"
                                            },
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium,
                                            color = when (device.connectionState) {
                                                com.example.ble.nearby.NearbyConnectionState.CONNECTED -> Color(0xFF2E7D32)
                                                com.example.ble.nearby.NearbyConnectionState.REQUESTING -> MaterialTheme.colorScheme.primary
                                                com.example.ble.nearby.NearbyConnectionState.DISCONNECTED -> MaterialTheme.colorScheme.onSurfaceVariant
                                            }
                                        )
                                        Text(
                                            text = "RSSI: ${device.rssi} dBm",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))

                                    when (device.connectionState) {
                                        com.example.ble.nearby.NearbyConnectionState.DISCONNECTED -> {
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                OutlinedButton(
                                                    onClick = { onNavigateToChat(device.macAddress, device.deviceName) },
                                                    modifier = Modifier.testTag("nearby_chat_button_${device.id}")
                                                ) {
                                                    Text("Chat")
                                                }
                                                Button(
                                                    onClick = { viewModel.nearbyBleManager.requestConnection(device.macAddress) },
                                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                                    modifier = Modifier.testTag("nearby_connect_button_${device.id}")
                                                ) {
                                                    Text("Connect")
                                                }
                                            }
                                        }
                                        com.example.ble.nearby.NearbyConnectionState.REQUESTING -> {
                                            OutlinedButton(
                                                onClick = { viewModel.nearbyBleManager.disconnect(device.macAddress) },
                                                modifier = Modifier.testTag("nearby_cancel_request_${device.id}")
                                            ) {
                                                Text("Cancel")
                                            }
                                        }
                                        com.example.ble.nearby.NearbyConnectionState.CONNECTED -> {
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                OutlinedButton(
                                                    onClick = { viewModel.nearbyBleManager.disconnect(device.macAddress) },
                                                    modifier = Modifier.testTag("nearby_disconnect_button_${device.id}")
                                                ) {
                                                    Text("Disconnect")
                                                }
                                                Button(
                                                    onClick = { onNavigateToChat(device.macAddress, device.deviceName) },
                                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                                                    modifier = Modifier.testTag("nearby_chat_button_${device.id}")
                                                ) {
                                                    Text("Chat")
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
