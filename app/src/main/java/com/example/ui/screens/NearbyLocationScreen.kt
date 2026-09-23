package com.example.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.model.NearbyLocationUpdate
import com.example.ui.NearbyLocationViewModel
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NearbyLocationScreen(
    onNavigateBack: () -> Unit,
    viewModel: NearbyLocationViewModel = hiltViewModel()
) {
    val locationsMap by viewModel.latestLocations.collectAsState()
    val locations = locationsMap.values.toList().sortedByDescending { it.timestamp }
    val connectedDevices by viewModel.connectedDevices.collectAsState()
    val sharingMacs by viewModel.sharingMacs.collectAsState()
    val context = LocalContext.current

    var permissionGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        permissionGranted = granted
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Nearby Locations") },
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
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            
            if (connectedDevices.isNotEmpty()) {
                item {
                    Text(
                        text = "Share My Location",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    if (!permissionGranted) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    "Location permission is required to share your location.",
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(onClick = {
                                    permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                                }) {
                                    Text("Grant Permission")
                                }
                            }
                        }
                    } else {
                        connectedDevices.forEach { device ->
                            val isSharing = sharingMacs.contains(device.macAddress)
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text(device.deviceName, fontWeight = FontWeight.SemiBold)
                                        Text(
                                            if (isSharing) "Location sharing ON" else "Location sharing OFF",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = if (isSharing) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Switch(
                                        checked = isSharing,
                                        onCheckedChange = { enable ->
                                            viewModel.toggleSharing(device.macAddress, enable)
                                        }
                                    )
                                }
                            }
                        }
                    }
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                }
            }

            if (locations.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No Nearby locations",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Location information will appear when a connected Smart SOS device shares its location.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else {
                items(locations, key = { it.messageId }) { locationUpdate ->
                    NearbyLocationCard(
                        locationUpdate = locationUpdate,
                        onOpenMap = { lat, lng ->
                            val uri = Uri.parse("geo:$lat,$lng?q=$lat,$lng")
                            val intent = Intent(Intent.ACTION_VIEW, uri)
                            try {
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                // Ignore gracefully if no map app exists
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun NearbyLocationCard(
    locationUpdate: NearbyLocationUpdate,
    onOpenMap: (Double, Double) -> Unit
) {
    // Ticker to refresh age dynamically
    var currentTime by remember { mutableStateOf(System.currentTimeMillis()) }
    
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            currentTime = System.currentTimeMillis()
        }
    }

    val diffSec = maxOf(0L, (currentTime - locationUpdate.timestamp) / 1000L)
    
    val ageText = when {
        diffSec < 10 -> "Updated just now"
        diffSec < 60 -> "Updated $diffSec seconds ago"
        diffSec < 120 -> "Updated 1 minute ago"
        else -> "Updated ${diffSec / 60} minutes ago"
    }

    val (statusColor, statusText) = when {
        diffSec < 30 -> MaterialTheme.colorScheme.primary to "Recent"
        diffSec <= 120 -> MaterialTheme.colorScheme.secondary to "Older"
        else -> MaterialTheme.colorScheme.error to "Stale"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = locationUpdate.senderDeviceName ?: "Nearby Smart SOS User",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = statusColor
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Location available • $statusText",
                    style = MaterialTheme.typography.bodyMedium,
                    color = statusColor,
                    fontWeight = FontWeight.SemiBold
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            val formattedLat = String.format(java.util.Locale.US, "%.6f", locationUpdate.latitude)
            val formattedLng = String.format(java.util.Locale.US, "%.6f", locationUpdate.longitude)
            
            Text(
                text = "$formattedLat, $formattedLng",
                style = MaterialTheme.typography.bodyLarge
            )
            
            val accuracyText = locationUpdate.accuracyMeters?.let {
                "Accuracy: ${String.format(java.util.Locale.US, "%.1f", it)} m"
            } ?: "Accuracy unavailable"
            
            Text(
                text = accuracyText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = ageText,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Button(
                    onClick = { onOpenMap(locationUpdate.latitude, locationUpdate.longitude) },
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text("View coordinates")
                }
            }
        }
    }
}
