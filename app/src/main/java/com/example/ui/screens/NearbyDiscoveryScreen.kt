package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
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
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val nearbyDevices by viewModel.nearbyBleManager.nearbyDevices.collectAsState()

    val permissions = mutableListOf<String>()
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        permissions.add(Manifest.permission.BLUETOOTH_SCAN)
        permissions.add(Manifest.permission.BLUETOOTH_CONNECT)
    } else {
        permissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    var hasPermissions by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        hasPermissions = results.values.all { it }
        if (hasPermissions) {
            viewModel.nearbyBleManager.startScanningForNearby()
        }
    }

    DisposableEffect(Unit) {
        val allGranted = permissions.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
        if (allGranted) {
            hasPermissions = true
            viewModel.nearbyBleManager.startScanningForNearby()
        } else {
            permissionLauncher.launch(permissions.toTypedArray())
        }

        onDispose {
            viewModel.nearbyBleManager.stopScanningForNearby()
        }
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
            } else if (nearbyDevices.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No nearby users detected.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Text(
                    text = "Discovered Users",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(nearbyDevices.values.toList().sortedByDescending { it.lastSeen }) { device ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
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
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(40.dp)
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Column {
                                    val safeId = device.macAddress.takeLast(4)
                                    Text(
                                        text = "Nearby User ($safeId)",
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "Signal: ${device.rssi} dBm",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    val timeString = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(device.lastSeen))
                                    Text(
                                        text = "Last seen: $timeString",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Spacer(modifier = Modifier.weight(1f))
                                
                                when (device.connectionState) {
                                    com.example.ble.nearby.NearbyConnectionState.DISCONNECTED -> {
                                        Button(
                                            onClick = { viewModel.nearbyBleManager.requestConnection(device.macAddress) },
                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                        ) {
                                            Text("Connect")
                                        }
                                    }
                                    com.example.ble.nearby.NearbyConnectionState.REQUESTING -> {
                                        OutlinedButton(
                                            onClick = { viewModel.nearbyBleManager.disconnect(device.macAddress) }
                                        ) {
                                            Text("Requesting...")
                                        }
                                    }
                                    com.example.ble.nearby.NearbyConnectionState.CONNECTED -> {
                                        Button(
                                            onClick = { viewModel.nearbyBleManager.disconnect(device.macAddress) },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                                        ) {
                                            Text("Connected")
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
