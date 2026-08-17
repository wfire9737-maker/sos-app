package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.GuardianViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    viewModel: GuardianViewModel,
    onNavigateBack: () -> Unit
) {
    val locationState by viewModel.currentLocation.collectAsState()
    val isTracking by viewModel.isTrackingLocation.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        if (!isTracking) {
            viewModel.startLocationTracking()
        }
    }

    val formatter = remember { SimpleDateFormat("MMM dd, yyyy HH:mm:ss", Locale.getDefault()) }
    val lastUpdated = formatter.format(Date(locationState.timestamp))

    val statusText = if (isTracking) {
        if (locationState.accuracy > 0 && locationState.accuracy < 100) "Acquired" else "Searching"
    } else {
        "Stopped"
    }
    
    val statusColor = when (statusText) {
        "Acquired" -> Color(0xFF4CAF50)
        "Searching" -> Color(0xFFFF9800)
        else -> MaterialTheme.colorScheme.error
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Live Location",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("map_back_btn")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Go Back"
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            if (isTracking) {
                                viewModel.stopLocationTracking()
                            } else {
                                viewModel.startLocationTracking()
                            }
                        },
                        modifier = Modifier.testTag("map_toggle_tracking")
                    ) {
                        Icon(
                            imageVector = if (isTracking) Icons.Default.LocationOn else Icons.Default.LocationOff,
                            contentDescription = "Toggle GPS",
                            tint = if (isTracking) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.LocationOn,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(64.dp).padding(bottom = 16.dp)
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Location Information",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    HorizontalDivider()

                    LocationDetailRow(label = "Status", value = statusText, valueColor = statusColor)
                    LocationDetailRow(
                        label = "Address",
                        value = if (locationState.address.isNotEmpty()) locationState.address else "Acquiring Address..."
                    )
                    LocationDetailRow(label = "Latitude", value = String.format(Locale.US, "%.5f", locationState.latitude))
                    LocationDetailRow(label = "Longitude", value = String.format(Locale.US, "%.5f", locationState.longitude))
                    LocationDetailRow(label = "Accuracy", value = "±${String.format(Locale.US, "%.1f", locationState.accuracy)} meters")
                    LocationDetailRow(label = "Last Updated", value = lastUpdated)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    val uri = "geo:${locationState.latitude},${locationState.longitude}?q=${locationState.latitude},${locationState.longitude}"
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uri))
                    context.startActivity(intent)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.Map, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                Text("Open in Google Maps", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun LocationDetailRow(label: String, value: String, valueColor: Color = MaterialTheme.colorScheme.onSurfaceVariant) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = valueColor,
            modifier = Modifier.weight(1f).padding(start = 16.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.End
        )
    }
}
