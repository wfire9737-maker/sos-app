import os

content = """package com.example.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.GuardianViewModel
import com.example.service.AuthState
import com.example.model.SosWorkflowState
import com.example.model.User
import com.example.ui.rememberLocationPermissionHandler

// Stitch UI Colors
val StitchBg = Color(0xFF0F1115)
val StitchCard = Color(0xFF1A1C23)
val StitchRed = Color(0xFFE5534B)
val StitchGreen = Color(0xFF20E070)
val StitchPurple = Color(0xFF6A6CFF)
val StitchTextMuted = Color(0xFFA0A0A5)
val StitchBottomNav = Color(0xFF13151A)

@Composable
fun HomeScreen(
    viewModel: GuardianViewModel,
    onNavigateToLogin: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToContacts: () -> Unit = {},
    onNavigateToDevicePairing: () -> Unit,
    onNavigateToMap: () -> Unit = {},
    onNavigateToBleTest: () -> Unit = {},
    onNavigateToEmergency: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    onNavigateToHistory: () -> Unit = {},
    onNavigateToAiDashboard: () -> Unit,
    onNavigateToDeviceMonitoring: () -> Unit,
    onNavigateToSettings: () -> Unit = {},
    onNavigateToAnalytics: () -> Unit,
    onNavigateToReports: () -> Unit,
    onNavigateToSafeCheckIn: () -> Unit = {}
) {
    val sosTriggerHandler = rememberLocationPermissionHandler {
        viewModel.triggerManualSOS()
        onNavigateToEmergency()
    }
    
    val authState by viewModel.authState.collectAsState()
    val isEsp32Connected by viewModel.isEsp32Connected.collectAsState()
    val sosWorkflowState by viewModel.sosWorkflowState.collectAsState()

    if (sosWorkflowState != SosWorkflowState.IDLE) {
        androidx.compose.ui.window.Dialog(onDismissRequest = {}) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = StitchCard,
                tonalElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier.padding(24.dp).fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(color = StitchRed)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Preparing SOS...",
                        color = Color.White,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }
    }

    Scaffold(
        bottomBar = {
            StitchBottomNav(
                onNavigateToHome = {},
                onNavigateToContacts = onNavigateToContacts,
                onNavigateToMap = onNavigateToMap,
                onNavigateToSettings = onNavigateToSettings,
                onNavigateToProfile = onNavigateToProfile
            )
        },
        containerColor = StitchBg
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp)
        ) {
            Spacer(modifier = Modifier.height(24.dp))
            StitchHeader(onNotificationsClick = onNavigateToNotifications)
            
            Spacer(modifier = Modifier.height(24.dp))
            StitchDeviceCard(
                isConnected = isEsp32Connected,
                onClick = onNavigateToDevicePairing
            )
            
            Spacer(modifier = Modifier.weight(1f))
            StitchSosButton(onSosClick = { sosTriggerHandler() })
            Spacer(modifier = Modifier.weight(1f))
            
            StitchFeaturesGrid()
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun StitchHeader(onNotificationsClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Color.DarkGray),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Person, contentDescription = "Profile", tint = Color.LightGray)
        }
        
        // Titles
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Armed & Guarded",
                color = StitchRed,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(6.dp).clip(CircleShape).background(StitchGreen)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "SYSTEM ACTIVE",
                    color = StitchGreen,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 1.sp
                )
            }
        }
        
        // Bell
        IconButton(onClick = onNotificationsClick) {
            Icon(Icons.Outlined.Notifications, contentDescription = "Notifications", tint = Color.White)
        }
    }
}

@Composable
fun StitchDeviceCard(isConnected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(StitchCard)
            .clickable { onClick() }
            .padding(20.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Memory, contentDescription = "Device", tint = StitchGreen)
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "Physical-SOS-ESP32",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Sync, contentDescription = "Sync", tint = StitchTextMuted, modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isConnected) "Last synced: Just now" else "Disconnected",
                        color = StitchTextMuted,
                        fontSize = 12.sp
                    )
                }
            }
        }
        
        Column(horizontalAlignment = Alignment.End) {
            // Signal Bars (mocking the look)
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                Box(modifier = Modifier.width(4.dp).height(8.dp).clip(RoundedCornerShape(2.dp)).background(StitchGreen))
                Box(modifier = Modifier.width(4.dp).height(12.dp).clip(RoundedCornerShape(2.dp)).background(StitchGreen))
                Box(modifier = Modifier.width(4.dp).height(16.dp).clip(RoundedCornerShape(2.dp)).background(StitchGreen))
            }
            Spacer(modifier = Modifier.height(8.dp))
            // Battery pill
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF2A2C35))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(StitchGreen))
                Spacer(modifier = Modifier.width(4.dp))
                Text("88%", color = Color.White, fontSize = 10.sp)
            }
        }
    }
}

@Composable
fun StitchSosButton(onSosClick: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition()
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000),
            repeatMode = RepeatMode.Reverse
        )
    )
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000),
            repeatMode = RepeatMode.Reverse
        )
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(contentAlignment = Alignment.Center) {
            // Outer pulsing ring
            Box(
                modifier = Modifier
                    .size(240.dp * scale)
                    .clip(CircleShape)
                    .border(1.dp, StitchRed.copy(alpha = alpha), CircleShape)
            )
            // Inner pulsing ring
            Box(
                modifier = Modifier
                    .size(180.dp * scale)
                    .clip(CircleShape)
                    .background(StitchRed.copy(alpha = 0.1f))
            )
            
            // Core SOS Button
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(StitchRed)
                    .clickable { onSosClick() },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "SOS",
                    color = Color(0xFF4A0000),
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 2.sp
                )
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Text(
            text = "Listener Active",
            color = StitchRed,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Hold central button for 3s to\ntrigger emergency",
            color = StitchTextMuted,
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp
        )
    }
}

@Composable
fun StitchFeaturesGrid() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        StitchFeatureCard(
            modifier = Modifier.weight(1f),
            title = "Keyword Listening",
            subtitle = "Voice SOS",
            icon = Icons.Default.Mic,
            iconTint = StitchPurple
        )
        StitchFeatureCard(
            modifier = Modifier.weight(1f),
            title = "Stealth Mode",
            subtitle = "Silent Panic",
            icon = Icons.Default.VisibilityOff,
            iconTint = StitchGreen
        )
    }
}

@Composable
fun StitchFeatureCard(
    modifier: Modifier,
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconTint: Color
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(StitchCard)
            .padding(20.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Color(0xFF2A2C35)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = title, tint = iconTint, modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = title,
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = subtitle,
            color = StitchTextMuted,
            fontSize = 12.sp
        )
    }
}

@Composable
fun StitchBottomNav(
    onNavigateToHome: () -> Unit,
    onNavigateToContacts: () -> Unit,
    onNavigateToMap: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToProfile: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(StitchBottomNav)
            .padding(vertical = 12.dp, horizontal = 24.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(StitchGreen.copy(alpha = 0.2f))
                .padding(12.dp)
        ) {
            Icon(Icons.Default.Home, contentDescription = "Home", tint = StitchGreen)
        }
        IconButton(onClick = onNavigateToContacts) {
            Icon(Icons.Outlined.Contacts, contentDescription = "Contacts", tint = StitchTextMuted)
        }
        IconButton(onClick = onNavigateToMap) {
            Icon(Icons.Outlined.Map, contentDescription = "Map", tint = StitchTextMuted)
        }
        IconButton(onClick = onNavigateToSettings) {
            Icon(Icons.Outlined.Settings, contentDescription = "Settings", tint = StitchTextMuted)
        }
        IconButton(onClick = onNavigateToProfile) {
            Icon(Icons.Outlined.Person, contentDescription = "Profile", tint = StitchTextMuted)
        }
    }
}
"""

with open("app/src/main/java/com/example/ui/screens/HomeScreen.kt", "w") as f:
    f.write(content)
