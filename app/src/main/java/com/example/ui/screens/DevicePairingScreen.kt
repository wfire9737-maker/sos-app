package com.example.ui.screens

import androidx.compose.animation.core.*
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
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Device
import com.example.ui.GuardianViewModel

private val StitchBg = Color(0xFF0F1115)
private val StitchCard = Color(0xFF1A1C23)
private val StitchRed = Color(0xFFE5534B)
private val StitchGreen = Color(0xFF20E070)
private val StitchPurple = Color(0xFF6A6CFF)
private val StitchTextMuted = Color(0xFFA0A0A5)
private val StitchDarkGray = Color(0xFF2A2A35)
private val StitchBottomNav = Color(0xFF13151A)

// Stitch UI Colors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DevicePairingScreen(
    viewModel: GuardianViewModel,
    onNavigateBack: () -> Unit
) {
    val devices by viewModel.devices.collectAsState()
    var isScanning by remember { mutableStateOf(true) }
    
    // Auto-stop scanning simulation for UI polish
    LaunchedEffect(Unit) {
        
        kotlinx.coroutines.delay(4000)
        isScanning = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Device Pairing", color = StitchRed, fontSize = 18.sp, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = StitchRed)
                    }
                },
                actions = {
                    IconButton(onClick = {}) {
                        Icon(Icons.Outlined.HelpOutline, contentDescription = "Help", tint = StitchTextMuted)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = StitchBg)
            )
        },
        containerColor = StitchBg
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Radar Section
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp),
                contentAlignment = Alignment.Center
            ) {
                StitchRadarAnimation(isScanning = isScanning)
            }
            
            Text(
                text = if (isScanning) "Scanning for devices..." else "Scan Complete",
                color = StitchPurple,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Ensure your safety device is turned on and nearby.",
                color = Color.White,
                fontSize = 14.sp,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Device List
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // If devices empty, show mock for UI design purposes, or the actual list if not empty
                val displayDevices = if (devices.isEmpty()) {
                    listOf(Device(deviceName = "Physical-SOS-ESP32", macAddress = "00:11:22"))
                } else devices
                
                items(displayDevices) { device ->
                    StitchDeviceItem(
                        device = device,
                        isSelected = device.deviceName.contains("ESP32", ignoreCase = true),
                        onConnect = { viewModel.connectDevice(device.macAddress) }
                    )
                }
            }
            
            // Bottom Panel
            StitchPairingBottomPanel()
        }
    }
}

@Composable
fun StitchRadarAnimation(isScanning: Boolean) {
    val infiniteTransition = rememberInfiniteTransition()
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    Box(contentAlignment = Alignment.Center) {
        // Outer rings
        Box(modifier = Modifier.size(240.dp).border(1.dp, StitchCard, CircleShape))
        Box(modifier = Modifier.size(160.dp).border(1.dp, StitchCard, CircleShape))
        
        // Rotating sweep
        if (isScanning) {
            Box(
                modifier = Modifier
                    .size(240.dp)
                    .clip(CircleShape)
                    .rotate(rotation)
                    .background(
                        Brush.sweepGradient(
                            colors = listOf(Color.Transparent, StitchPurple.copy(alpha = 0.4f)),
                            center = androidx.compose.ui.geometry.Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY) // Not exact but compose sweep uses relative offsets
                        )
                    )
            )
        }
        
        // Center Bluetooth icon
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(StitchCard)
                .border(1.dp, StitchPurple.copy(alpha = 0.5f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Bluetooth, contentDescription = "Bluetooth", tint = StitchPurple, modifier = Modifier.size(28.dp))
        }
        
        // Simulated dots on radar
        Box(modifier = Modifier.size(240.dp)) {
            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(StitchPurple).align(Alignment.TopEnd).offset(x = (-40).dp, y = 40.dp))
            Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(StitchPurple.copy(alpha = 0.5f)).align(Alignment.BottomStart).offset(x = 60.dp, y = (-20).dp))
        }
    }
}

@Composable
fun StitchDeviceItem(device: Device, isSelected: Boolean, onConnect: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(StitchCard)
            .border(
                width = 2.dp,
                color = if (isSelected) StitchPurple else Color.Transparent,
                shape = RoundedCornerShape(16.dp)
            )
            .clickable { onConnect() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(StitchBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Watch, contentDescription = "Device", tint = StitchPurple)
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = device.deviceName,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.SignalCellularAlt, contentDescription = "Signal", tint = StitchTextMuted, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "RSSI: -45dBm (Strong)",
                    color = StitchTextMuted,
                    fontSize = 12.sp
                )
            }
        }
        
        if (isSelected) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(StitchGreen.copy(alpha = 0.2f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text("PAIRED", color = StitchGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Icon(Icons.Default.CheckCircle, contentDescription = "Selected", tint = StitchPurple)
            }
        }
    }
}

@Composable
fun StitchPairingBottomPanel() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(StitchBg)
            .padding(24.dp)
    ) {
        // Active Mapping Summary
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .border(1.dp, StitchCard, RoundedCornerShape(16.dp))
                .padding(16.dp)
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Settings, contentDescription = "Settings", tint = StitchTextMuted, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Active Mapping Summary", color = Color.White, fontSize = 12.sp)
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("1x Click", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text("Location", color = StitchTextMuted, fontSize = 10.sp)
                    }
                    Divider(modifier = Modifier.width(1.dp).height(24.dp), color = StitchDarkGray)
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("2x Click", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text("Fake Call", color = StitchTextMuted, fontSize = 10.sp)
                    }
                    Divider(modifier = Modifier.width(1.dp).height(24.dp), color = StitchDarkGray)
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Long Hold", color = StitchRed, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text("SOS Trigger", color = StitchRed, fontSize = 10.sp)
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Button(
            onClick = { /* Connect */ },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = StitchPurple),
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(Icons.Default.Link, contentDescription = "Connect")
            Spacer(modifier = Modifier.width(8.dp))
            Text("Connect & Calibrate", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}
