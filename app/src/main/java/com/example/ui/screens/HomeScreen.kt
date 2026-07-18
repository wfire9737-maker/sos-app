package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Alert
import com.example.model.Device
import com.example.service.AuthState
import com.example.ui.GuardianViewModel
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: GuardianViewModel,
    onNavigateToLogin: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToContacts: () -> Unit,
    onNavigateToDevicePairing: () -> Unit,
    onNavigateToMap: () -> Unit,
    onNavigateToEmergency: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToAiDashboard: () -> Unit,
    onNavigateToDeviceMonitoring: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToAnalytics: () -> Unit,
    onNavigateToReports: () -> Unit
) {
    val authState by viewModel.authState.collectAsState()
    val alerts by viewModel.alerts.collectAsState()
    val devices by viewModel.devices.collectAsState()
    val emergencySession by viewModel.emergencySession.collectAsState()
    val notifications by viewModel.notifications.collectAsState()
    val isDemo = viewModel.isDemoMode

    val fallState by viewModel.fallState.collectAsState()
    val fallCountdown by viewModel.fallCountdown.collectAsState()

    val unreadCount = remember(notifications) { notifications.count { !it.isRead } }

    val currentUser = (authState as? AuthState.Success)?.user ?: com.example.model.User(name = "Guardian User")

    var showBondDialog by remember { mutableStateOf(false) }
    var showResolveDialog by remember { mutableStateOf<Alert?>(null) }
    
    // Pulse animation timer for radar & SOS button
    var pulseTrigger by remember { mutableStateOf(0f) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(50)
            pulseTrigger = (pulseTrigger + 0.05f) % 2f
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFDFBFF))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
        ) {
            // --- PREMIUM POLISHED HEADER ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Smart SOS App",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1B1B1F),
                        letterSpacing = (-0.5).sp
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF22C55E))
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "System Active",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF44474E)
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Quick Action Alert Trigger Status
                    IconButton(
                        onClick = { viewModel.triggerManualSOS() },
                        modifier = Modifier
                            .size(40.dp)
                            .background(Color(0xFFF3F3FA), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Emergency,
                            contentDescription = "Trigger Quick SOS",
                            tint = Color(0xFF0061A4),
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    // Premium Notification Bell Icon with Badge
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFF3F3FA), CircleShape)
                            .clickable { onNavigateToNotifications() }
                            .testTag("notification_bell_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = "Notifications",
                            tint = Color(0xFF0061A4),
                            modifier = Modifier.size(22.dp)
                        )
                        if (unreadCount > 0) {
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .background(Color.Red, CircleShape)
                                    .align(Alignment.TopEnd),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = unreadCount.toString(),
                                    color = Color.White,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    // Settings Button
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFF3F3FA), CircleShape)
                            .clickable { onNavigateToSettings() }
                            .testTag("settings_header_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = Color(0xFF0061A4),
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    // Profile initials bubble (JD) matching HTML
                    val initials = if (currentUser.name.isNotBlank()) {
                        currentUser.name.split(" ")
                            .mapNotNull { it.firstOrNull()?.uppercaseChar() }
                            .joinToString("")
                            .take(2)
                    } else {
                        "GU"
                    }
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFD1E1FF))
                            .border(1.dp, Color(0xFF74777F), CircleShape)
                            .clickable { onNavigateToProfile() }
                            .testTag("profile_avatar_bubble"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = initials,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF001D35)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    // Log Out Button
                    IconButton(
                        onClick = { viewModel.logout() },
                        modifier = Modifier
                            .size(40.dp)
                            .background(Color(0xFFFFF0F1), CircleShape)
                            .testTag("logout_header_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ExitToApp,
                            contentDescription = "Log Out",
                            tint = Color(0xFFD32F2F),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            // Scrollable Dashboard Body (Adjusted height to weight 1f to dock footer perfectly)
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // --- ACTIVE EMERGENCY PANEL BANNER (Blinking Red) ---
                if (emergencySession.isActive) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF8B0000)),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onNavigateToEmergency() }
                                .border(2.dp, Color.White, RoundedCornerShape(16.dp))
                                .shadow(8.dp, RoundedCornerShape(16.dp))
                                .testTag("active_emergency_banner")
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .background(Color.White, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("🚨", fontSize = 20.sp)
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "CRITICAL LIVE EMERGENCY",
                                        fontWeight = FontWeight.Black,
                                        fontSize = 14.sp,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "Tap immediately to access telemetry, maps & controls.",
                                        fontSize = 11.sp,
                                        color = Color.LightGray
                                    )
                                }
                                Text("GO →", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 12.sp)
                            }
                        }
                    }
                }

                // --- SOS RADAR MAP SCANNER (Wow-factor) ---
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .border(1.dp, MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f), RoundedCornerShape(24.dp))
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            // Custom painted scanning radar map
                            val radarStrokeColor = EmergencyRed.copy(alpha = 0.2f)
                            val scanningSweepColor = EmergencyRedLight.copy(alpha = 0.15f)
                            val activeRubyColor = EmergencyRed
                            val userDotColor = SafetyBlue
                            val gridLineColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.05f)

                            val activeAlerts = alerts.filter { it.status == "ACTIVE" }

                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val centerWidth = size.width / 2f
                                val centerHeight = size.height / 2f
                                val maxRadius = size.height * 0.9f

                                // Draw grids
                                drawRect(color = Color.Transparent)
                                for (i in 1..4) {
                                    drawCircle(
                                        color = radarStrokeColor,
                                        radius = (maxRadius / 4f) * i,
                                        center = center,
                                        style = Stroke(width = 1.dp.toPx())
                                    )
                                }

                                // Cross gridlines
                                drawLine(color = gridLineColor, start = androidx.compose.ui.geometry.Offset(centerWidth, 0f), end = androidx.compose.ui.geometry.Offset(centerWidth, size.height), strokeWidth = 1.dp.toPx())
                                drawLine(color = gridLineColor, start = androidx.compose.ui.geometry.Offset(0f, centerHeight), end = androidx.compose.ui.geometry.Offset(size.width, centerHeight), strokeWidth = 1.dp.toPx())

                                // Radar sweeping hand simulation
                                val angleInRadians = (pulseTrigger * Math.PI).toFloat()
                                val endX = centerWidth + (maxRadius * Math.cos(angleInRadians.toDouble())).toFloat()
                                val endY = centerHeight + (maxRadius * Math.sin(angleInRadians.toDouble())).toFloat()
                                drawLine(
                                    color = scanningSweepColor,
                                    start = center,
                                    end = androidx.compose.ui.geometry.Offset(endX, endY),
                                    strokeWidth = 3.dp.toPx()
                                )

                                // Plot user position
                                drawCircle(
                                    color = userDotColor,
                                    radius = 8.dp.toPx(),
                                    center = center
                                )
                                drawCircle(
                                    color = userDotColor.copy(alpha = 0.3f),
                                    radius = 16.dp.toPx() * (pulseTrigger % 1f),
                                    center = center,
                                    style = Stroke(width = 2.dp.toPx())
                                )

                                // Plot active alerts dynamically
                                activeAlerts.forEachIndexed { idx, alert ->
                                    // Map coordinates to simulated offset
                                    val offsetMultiplier = 0.15f + (idx * 0.1f)
                                    val deltaX = if (idx % 2 == 0) centerWidth * offsetMultiplier else -centerWidth * offsetMultiplier
                                    val deltaY = if (idx % 3 == 0) centerHeight * offsetMultiplier else -centerHeight * offsetMultiplier
                                    val alertCenter = androidx.compose.ui.geometry.Offset(centerWidth + deltaX, centerHeight + deltaY)

                                    drawCircle(
                                        color = activeRubyColor,
                                        radius = 6.dp.toPx(),
                                        center = alertCenter
                                    )
                                    // Pulse aura around alerts
                                    drawCircle(
                                        color = activeRubyColor.copy(alpha = 0.4f),
                                        radius = 18.dp.toPx() * (pulseTrigger % 1f),
                                        center = alertCenter,
                                        style = Stroke(width = 1.5.dp.toPx())
                                    )
                                }
                            }

                            // Info Overlay
                            Column(
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(14.dp)
                            ) {
                                Text(
                                    text = "LIVE SOS RADAR",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = EmergencyRed,
                                    letterSpacing = 1.sp
                                )
                                Text(
                                    text = "Scan Area: 5km (Active GPS Monitoring)",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(12.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(EmergencyRed.copy(alpha = 0.12f))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "${alerts.count { it.status == "ACTIVE" }} ACTIVE DISTRESS",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = EmergencyRed
                                )
                            }
                        }
                    }
                }

                // --- PROFESSIONAL POLISH TACTILE SOS CARD ---
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = ProfEmergencyBg
                        ),
                        shape = RoundedCornerShape(28.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                            .shadow(1.dp, RoundedCornerShape(28.dp)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            // SOS Button Wrapper with pulse ring animations
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(170.dp)
                                    .padding(bottom = 12.dp)
                            ) {
                                // Subtle pulse aura around the button
                                Box(
                                    modifier = Modifier
                                        .size(130.dp + (30.dp * (pulseTrigger % 1f)))
                                        .clip(CircleShape)
                                        .background(ProfSosButtonBorder.copy(alpha = 0.35f * (1f - (pulseTrigger % 1f))))
                                )
                                
                                // Tactile button
                                Button(
                                    onClick = { viewModel.triggerManualSOS() },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = ProfSosButtonBg,
                                        contentColor = ProfSosButtonText
                                    ),
                                    shape = CircleShape,
                                    modifier = Modifier
                                        .size(128.dp)
                                        .border(8.dp, ProfSosButtonBorder, CircleShape)
                                        .shadow(8.dp, CircleShape)
                                        .testTag("sos_button")
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Text(
                                            text = "🆘",
                                            fontSize = 32.sp,
                                            modifier = Modifier.padding(bottom = 2.dp)
                                        )
                                        Text(
                                            text = "SOS",
                                            fontSize = 20.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = ProfSosButtonText,
                                            letterSpacing = 1.sp
                                        )
                                    }
                                }
                            }
                            
                            Text(
                                text = "Press and hold for 3 seconds",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1B1B1F)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Emergency services will be notified",
                                fontSize = 12.sp,
                                color = Color(0xFF49454F)
                            )
                        }
                    }
                }

                // --- 2-COLUMN STATUS GRID (Location & Firebase status) ---
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Location Card
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF3F3FA)),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .weight(1f)
                                .border(1.dp, Color(0xFFDDE2F0), RoundedCornerShape(16.dp))
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Text("📍", fontSize = 18.sp)
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF22C55E)) // green-500
                                    )
                                }
                                Text(
                                    text = "LOCATION",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF44474E),
                                    letterSpacing = 0.5.sp
                                )
                                Text(
                                    text = "Active Tracking",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1B1B1F)
                                )
                            }
                        }

                        // Firebase Card
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF3F3FA)),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .weight(1f)
                                .border(1.dp, Color(0xFFDDE2F0), RoundedCornerShape(16.dp))
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Text("🔥", fontSize = 18.sp)
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(if (isDemo) Color(0xFFF57C00) else Color(0xFF22C55E))
                                    )
                                }
                                Text(
                                    text = "FIREBASE",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF44474E),
                                    letterSpacing = 0.5.sp
                                )
                                Text(
                                    text = if (isDemo) "Local Linked" else "Cloud Linked",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1B1B1F)
                                )
                            }
                        }
                    }
                }

                // --- PRIMARY GUARDIAN CARD ---
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, Color(0xFFC4C6D0), RoundedCornerShape(16.dp))
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(16.dp)
                                .fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFF0061A4)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("🛡️", fontSize = 20.sp)
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Primary Guardian",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1B1B1F)
                                )
                                Text(
                                    text = "Sarah Jenkins • +1 234 567 890",
                                    fontSize = 12.sp,
                                    color = Color(0xFF44474E)
                                )
                            }
                            Text(
                                text = "→",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0061A4)
                            )
                        }
                    }
                }

                // --- ESP32 SMART-BAND SOS SIMULATOR DECK ---
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF0F4F8)),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.5.dp, Color(0xFFB0C4DE), RoundedCornerShape(20.dp))
                            .shadow(2.dp, RoundedCornerShape(20.dp))
                            .testTag("esp32_simulator_card")
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text("🛰️", fontSize = 20.sp)
                                    Column {
                                        Text(
                                            text = "ESP32 SMART-BAND EMULATOR",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = Color(0xFF1E3A8A)
                                        )
                                        Text(
                                            text = "Simulate physical device broadcasts",
                                            fontSize = 10.sp,
                                            color = Color(0xFF475569)
                                        )
                                    }
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Color(0xFF3B82F6).copy(alpha = 0.15f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text("ACTIVE BIND", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2563EB))
                                }
                            }

                            HorizontalDivider(color = Color(0xFFCBD5E1))

                            Text(
                                text = "Pressing these buttons will instantly broadcast simulated hardware events to trigger the Emergency SOS system.",
                                fontSize = 11.sp,
                                color = Color(0xFF64748B)
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Simulate Manual SOS Button Press
                                Button(
                                    onClick = { viewModel.triggerEsp32SOS("ESP32_BUTTON") },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(44.dp)
                                        .testTag("simulate_esp32_sos_button"),
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Icon(Icons.Default.Emergency, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.White)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Simulate SOS", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }

                                // Simulate Fall Detected Event
                                Button(
                                    onClick = { viewModel.fallDetectionService.triggerSimulatedFall() },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B)),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(44.dp)
                                        .testTag("simulate_esp32_fall_button"),
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Icon(Icons.Default.DirectionsRun, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.White)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Simulate Fall", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }
                        }
                    }
                }

                // --- AI INTELLIGENCE SYSTEM CARD ---
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.25f)
                        ),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigateToAiDashboard() }
                            .testTag("ai_summary_preview_card")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("🤖", fontSize = 24.sp)
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "AI Incident Analytics",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                                Text(
                                    text = "MPU6050 Accelerometer & Gyroscope pattern matching logs. Tap to view live waveform telemetry.",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.75f)
                                )
                            }

                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = "Go to AI Analytics",
                                tint = MaterialTheme.colorScheme.tertiary
                            )
                        }
                    }
                }

                // --- SYSTEM ANALYTICS DASHBOARD CARD ---
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
                        ),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigateToAnalytics() }
                            .testTag("system_analytics_preview_card")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("📈", fontSize = 24.sp)
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Analytics Dashboard",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = "Audits, severity distribution, incident frequencies, response times, and battery health trends.",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                )
                            }

                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = "Go to Analytics",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                // --- SAFETY & ACTIVITY REPORTS CARD ---
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.2f)
                        ),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigateToReports() }
                            .testTag("reports_preview_card")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("📋", fontSize = 24.sp)
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Safety & Activity Reports",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                Text(
                                    text = "Generate commercial-grade PDF/CSV safety audit logs, print summaries, and share maps/event details.",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                                )
                            }

                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = "Go to Reports",
                                tint = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                }

                // --- ACTIVE EMERGENCY ALERTS FEED ---
                item {
                    Text(
                        text = "Active SOS Distress Signals",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                val activeAlerts = alerts.filter { it.status == "ACTIVE" }
                if (activeAlerts.isEmpty()) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Security,
                                    contentDescription = null,
                                    tint = SafetyGreen,
                                    modifier = Modifier.size(36.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "All Sectors Secured",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SafetyGreen
                                )
                                Text(
                                    text = "No active emergency beacon detections in range.",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                } else {
                    items(activeAlerts, key = { it.id }) { alert ->
                        AlertCard(alert = alert, onResolveClick = { showResolveDialog = alert })
                    }
                }

                // --- RECOVERY RESOLVED HISTORIC ALERTS ---
                val resolvedAlerts = alerts.filter { it.status == "RESOLVED" }.take(3)
                if (resolvedAlerts.isNotEmpty()) {
                    item {
                        Text(
                            text = "Recently Mitigated Hazards",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                    items(resolvedAlerts, key = { it.id }) { alert ->
                        ResolvedAlertCard(alert = alert)
                    }
                }

                // --- CONNECTED ESP32 HARDWARE PANELS ---
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Connected ESP32 Wearables",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Button(
                            onClick = onNavigateToDevicePairing,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("BOND DEVICE", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                if (devices.isEmpty()) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Watch,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(32.dp)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "No Wearable Connected",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Register an ESP32 hardware beacon to enable automatic fall-detection triggers.",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                } else {
                    items(devices, key = { it.deviceId }) { device ->
                        DeviceCard(
                            device = device,
                            onUnbond = { viewModel.unbondDevice(device.deviceId) },
                            onSimulateClick = { triggerType ->
                                viewModel.triggerESP32SimulatedSOS(triggerType)
                            },
                            onMonitorClick = onNavigateToDeviceMonitoring
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }

            // --- DEVICE STATUS FOOTER ---
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF7F9FF))
                    .border(BorderStroke(1.dp, Color(0xFFDDE2F0)), shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                    .padding(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "DEVICE STATUS",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF44474E),
                            letterSpacing = 1.sp
                        )
                        val connectedCount = devices.size
                        Text(
                            text = if (connectedCount > 0) "${devices.first().deviceName} Connected" else "ESP32-942 Connected",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF0061A4)
                        )
                    }
                    // Progress indicator / battery health bar
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFDEE2F9))
                    ) {
                        val progressFraction = if (devices.isNotEmpty()) devices.first().batteryLevel / 100f else 0.75f
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(progressFraction)
                                .clip(CircleShape)
                                .background(Color(0xFF0061A4))
                        )
                    }
                }
            }

            // --- BOTTOM NAVIGATION BAR ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp)
                    .background(Color(0xFFF3F3FA))
                    .border(BorderStroke(1.dp, Color(0xFFDDE2F0)))
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Home (Active Tab)
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.clickable { }
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFFD1E1FF))
                            .padding(horizontal = 20.dp, vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("🏠", fontSize = 16.sp)
                    }
                    Text("Home", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF001D35))
                }

                // Map
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .clickable { onNavigateToMap() }
                        .testTag("map_nav_button")
                ) {
                    Text("🗺️", fontSize = 18.sp)
                    Text("Map", fontSize = 10.sp, fontWeight = FontWeight.Medium, color = Color(0xFF44474E))
                }

                // Guardians
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .clickable { onNavigateToContacts() }
                        .testTag("guardians_nav_button")
                ) {
                    Text("👥", fontSize = 18.sp)
                    Text("Guardians", fontSize = 10.sp, fontWeight = FontWeight.Medium, color = Color(0xFF44474E))
                }

                // History Log
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .clickable { onNavigateToHistory() }
                        .testTag("history_nav_button")
                ) {
                    Text("📜", fontSize = 18.sp)
                    Text("History", fontSize = 10.sp, fontWeight = FontWeight.Medium, color = Color(0xFF44474E))
                }

                // AI Dashboard
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .clickable { onNavigateToAiDashboard() }
                        .testTag("ai_dashboard_nav_button")
                ) {
                    Text("🤖", fontSize = 18.sp)
                    Text("AI Panel", fontSize = 10.sp, fontWeight = FontWeight.Medium, color = Color(0xFF44474E))
                }

                // Settings
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .clickable { onNavigateToProfile() }
                        .testTag("settings_nav_button")
                ) {
                    Text("⚙️", fontSize = 18.sp)
                    Text("Passport", fontSize = 10.sp, fontWeight = FontWeight.Medium, color = Color(0xFF44474E))
                }
            }
        }

        // --- BOND DIALOG ---
        if (showBondDialog) {
            BondDeviceDialog(
                onDismiss = { showBondDialog = false },
                onBondConfirm = { nickname, mac ->
                    viewModel.bondDevice(nickname, mac)
                    showBondDialog = false
                }
            )
        }

        // --- RESOLVE DIALOG ---
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

        // --- FALL DETECTION AUTOMATION MODAL OVERLAY ---
        if (fallState == "FALL_COUNTDOWN") {
            FallCountdownDialog(
                secondsLeft = fallCountdown,
                onCancel = { viewModel.fallDetectionService.cancelFallCountdown() }
            )
        }
    }
}

// --- COMPOSE WIDGET COMPONENTS ---

@Composable
fun AlertCard(alert: Alert, onResolveClick: () -> Unit) {
    val dateString = remember(alert.timestamp) {
        val sdf = SimpleDateFormat("hh:mm:ss a", Locale.getDefault())
        sdf.format(Date(alert.timestamp))
    }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = EmergencyRed.copy(alpha = 0.08f)
        ),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(2.dp, EmergencyRed, RoundedCornerShape(16.dp))
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(EmergencyRed),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when (alert.triggerType) {
                        "FALL_DETECTED" -> Icons.Default.DirectionsRun
                        "ESP32_BUTTON" -> Icons.Default.RadioButtonChecked
                        else -> Icons.Default.TouchApp
                    },
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = alert.userName,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = dateString,
                        fontSize = 11.sp,
                        color = EmergencyRed,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = "Phone: ${alert.userPhone}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(EmergencyRed.copy(alpha = 0.15f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = alert.triggerType,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = EmergencyRed
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    Text(
                        text = "GPS: ${String.format("%.4f", alert.latitude)}, ${String.format("%.4f", alert.longitude)}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = onResolveClick,
                modifier = Modifier
                    .size(36.dp)
                    .background(EmergencyRed, CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Resolve Signal",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun ResolvedAlertCard(alert: Alert) {
    val dateString = remember(alert.timestamp) {
        val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
        sdf.format(Date(alert.timestamp))
    }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = SafetyGreen,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = alert.userName,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Text(
                    text = dateString,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (alert.notes.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Resolution report: \"${alert.notes}\"",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 16.sp
                )
                Text(
                    text = "Resolved by: ${alert.resolvedBy}",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = SafetyGreen
                )
            }
        }
    }
}

@Composable
fun DeviceCard(
    device: Device,
    onUnbond: () -> Unit,
    onSimulateClick: (String) -> Unit,
    onMonitorClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Watch,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = device.deviceName,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "MAC: ${device.macAddress}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                        )
                    }
                }

                IconButton(onClick = onUnbond) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Unbond Wearable",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Divider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.08f))

            Spacer(modifier = Modifier.height(12.dp))

            // Battery and Hardware state indicator row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.BatteryChargingFull,
                        contentDescription = null,
                        tint = SafetyGreen,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${device.batteryLevel}% Battery",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = SafetyGreen
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(SafetyGreen.copy(alpha = 0.12f))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "MONITORING ACTIVE",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = SafetyGreen
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // SIMULATOR CONTROLS
            Text(
                text = "Simulate ESP32 Sensor Hardware Triggers:",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { onSimulateClick("FALL_DETECTED") },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = AlertOrange),
                    border = BorderStroke(1.dp, AlertOrange.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.weight(1f).height(32.dp)
                ) {
                    Icon(Icons.Default.DirectionsRun, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Impact / Fall", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = { onSimulateClick("ESP32_BUTTON") },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = EmergencyRed),
                    border = BorderStroke(1.dp, EmergencyRed.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.weight(1f).height(32.dp)
                ) {
                    Icon(Icons.Default.RadioButtonChecked, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Hardware Click", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = onMonitorClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(38.dp)
                    .testTag("device_monitor_button_${device.deviceId}")
            ) {
                Icon(
                    imageVector = Icons.Default.Watch,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("📈 View System Diagnostics", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun BondDeviceDialog(
    onDismiss: () -> Unit,
    onBondConfirm: (String, String) -> Unit
) {
    var nickname by remember { mutableStateOf("") }
    var macAddress by remember { mutableStateOf("") }
    var inputError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.BluetoothSearching, contentDescription = null, tint = EmergencyRed)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Bond Wearable Band", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Pair a simulated ESP32 SOS wristband or ring via local Bluetooth BLE emulator.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = nickname,
                    onValueChange = { nickname = it },
                    label = { Text("Device Nickname") },
                    placeholder = { Text("e.g. My Guardian Band v1") },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = EmergencyRed, focusedLabelColor = EmergencyRed),
                    modifier = Modifier.fillMaxWidth().testTag("bond_name")
                )

                OutlinedTextField(
                    value = macAddress,
                    onValueChange = { macAddress = it },
                    label = { Text("Bluetooth MAC Address") },
                    placeholder = { Text("e.g. 30:AE:A4:07:0D:64") },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = EmergencyRed, focusedLabelColor = EmergencyRed),
                    modifier = Modifier.fillMaxWidth().testTag("bond_mac")
                )

                inputError?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (nickname.isBlank() || macAddress.isBlank()) {
                        inputError = "All fields are required."
                    } else if (!macAddress.contains(":")) {
                        inputError = "Please enter a valid MAC address (separated by colons)."
                    } else {
                        onBondConfirm(nickname.trim(), macAddress.trim())
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = EmergencyRed),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("BOND DEVICE", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCEL", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    )
}

@Composable
fun ResolveAlertDialog(
    alert: Alert,
    onDismiss: () -> Unit,
    onResolveConfirm: (String) -> Unit
) {
    var notes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AssignmentTurnedIn, contentDescription = null, tint = SafetyGreen)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Resolve Distress SOS", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Provide a brief mitigation/rescue report to resolve this active alert for ${alert.userName}.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Mitigation Notes") },
                    placeholder = { Text("e.g. Safety unit arrived. Accidental hardware button trigger by client inside pocket. Safe.") },
                    singleLine = false,
                    maxLines = 4,
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = SafetyGreen, focusedLabelColor = SafetyGreen),
                    modifier = Modifier.fillMaxWidth().testTag("resolve_notes")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onResolveConfirm(notes) },
                colors = ButtonDefaults.buttonColors(containerColor = SafetyGreen),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("RESOLVE EMERGENCY", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCEL", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    )
}
