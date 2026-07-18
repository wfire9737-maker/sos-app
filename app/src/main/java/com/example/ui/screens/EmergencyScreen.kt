package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.GuardianViewModel
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmergencyScreen(
    viewModel: GuardianViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToMap: () -> Unit
) {
    val context = LocalContext.current
    val emergencySession by viewModel.emergencySession.collectAsState()
    val authState by viewModel.authState.collectAsState()

    // Real-time ticking elapsed timer since the SOS started
    var elapsedSeconds by remember { mutableStateOf(0L) }

    LaunchedEffect(emergencySession.startTimeMs) {
        if (emergencySession.startTimeMs > 0L) {
            while (true) {
                val currentMs = System.currentTimeMillis()
                elapsedSeconds = (currentMs - emergencySession.startTimeMs) / 1000
                delay(1000)
            }
        } else {
            elapsedSeconds = 0L
        }
    }

    // Format elapsed time as mm:ss
    val formattedElapsedTime = remember(elapsedSeconds) {
        val minutes = elapsedSeconds / 60
        val seconds = elapsedSeconds % 60
        String.format("%02d:%02d", minutes, seconds)
    }

    // Dynamic warning pulsing animation for the background/title
    val infiniteTransition = rememberInfiniteTransition(label = "Pulse")
    val pulsingAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseAlpha"
    )

    // Formatted Time and Date of trigger
    val triggerDateTime = remember(emergencySession.startTimeMs) {
        val ms = if (emergencySession.startTimeMs > 0L) emergencySession.startTimeMs else System.currentTimeMillis()
        val timeFormat = SimpleDateFormat("hh:mm:ss a", Locale.getDefault())
        val dateFormat = SimpleDateFormat("EEEE, MMMM dd, yyyy", Locale.getDefault())
        Pair(timeFormat.format(Date(ms)), dateFormat.format(Date(ms)))
    }

    // Active User Information
    val currentUser = (authState as? com.example.service.AuthState.Success)?.user
    val userName = emergencySession.activeAlert?.userName ?: currentUser?.name ?: "Marcus Vance"
    val userPhone = emergencySession.activeAlert?.userPhone ?: currentUser?.phone ?: "+1-555-0143"
    val medicalInfo = currentUser?.medicalInfo ?: "Type-1 Diabetes, High blood pressure, penicillin allergy"

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "🔴 LIVE EMERGENCY SOS",
                            fontWeight = FontWeight.Black,
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("emergency_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Go Back",
                            tint = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color(0xFF8B0000).copy(alpha = pulsingAlpha),
                    titleContentColor = Color.White
                )
            )
        },
        containerColor = Color(0xFF0F0404) // Ultra dark warning theme
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // --- HEADER METRIC BAR (ELAPSED TIME & EMERGENCY LEVEL) ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E0B0B)),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color(0xFFE53935).copy(alpha = 0.4f))
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("ELAPSED TIME", fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                        Text(
                            text = formattedElapsedTime,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFFFF5252),
                            modifier = Modifier.testTag("elapsed_time_ticker")
                        )
                    }
                }

                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E0B0B)),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color(0xFFE53935).copy(alpha = 0.4f))
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("EMERGENCY LEVEL", fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                        Text(
                            text = emergencySession.emergencyLevel,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFFB300),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.testTag("emergency_level_badge")
                        )
                    }
                }
            }

            // --- USER PHOTO & BIOMETRICS PROFILE ---
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF160808)),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, Color(0xFFE53935).copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Styled avatar as photo fallback
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(Color(0xFFE53935), Color(0xFF8B0000))
                                )
                            )
                            .border(2.dp, Color.White, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (userName.isNotEmpty()) userName.take(2).uppercase() else "SOS",
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                    }

                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = userName,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            modifier = Modifier.testTag("user_name_text")
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Device Bind ID: ${emergencySession.deviceId}",
                            fontSize = 12.sp,
                            color = Color.LightGray,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.testTag("device_id_text")
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Medical Info: $medicalInfo",
                            fontSize = 11.sp,
                            color = Color(0xFFFF8A80),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // --- REAL-TIME TELEMETRY STATS & AI CONFIDENCE DIAL ---
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF140707)),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, Color(0xFF44474E).copy(alpha = 0.4f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "🛰️ DEVICE TELEMETRY DECK",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.Gray
                    )

                    HorizontalDivider(color = Color(0xFFE53935).copy(alpha = 0.2f))

                    // AI Fall Detection Confidence Indicator
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Psychology, contentDescription = null, tint = Color(0xFF29B6F6))
                            Text("AI Fall Confidence", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                        Text(
                            text = "${emergencySession.aiConfidence}% ACCURACY",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF66BB6A),
                            modifier = Modifier.testTag("ai_confidence_score")
                        )
                    }

                    // Battery, GPS, Internet Status lines
                    TelemetryRow(
                        icon = Icons.Default.BatteryAlert,
                        label = "ESP32 Battery",
                        value = "${emergencySession.batteryLevel}% remaining",
                        valueColor = if (emergencySession.batteryLevel > 30) Color.Green else Color.Red,
                        tag = "battery_status"
                    )

                    TelemetryRow(
                        icon = Icons.Default.GpsFixed,
                        label = "GPS Fix Quality",
                        value = emergencySession.gpsStatus,
                        valueColor = Color(0xFF29B6F6),
                        tag = "gps_status"
                    )

                    TelemetryRow(
                        icon = Icons.Default.NetworkCheck,
                        label = "Network Status",
                        value = emergencySession.internetStatus,
                        valueColor = Color(0xFFAB47BC),
                        tag = "internet_status"
                    )

                    TelemetryRow(
                        icon = Icons.Default.AccessTime,
                        label = "Trigger Time",
                        value = triggerDateTime.first,
                        valueColor = Color.LightGray,
                        tag = "trigger_time"
                    )

                    TelemetryRow(
                        icon = Icons.Default.CalendarToday,
                        label = "Trigger Date",
                        value = triggerDateTime.second,
                        valueColor = Color.LightGray,
                        tag = "trigger_date"
                    )
                }
            }

            // --- LIVE MAP LOCATION & COORDINATES ---
            val lat = emergencySession.activeAlert?.latitude ?: 37.7749
            val lng = emergencySession.activeAlert?.longitude ?: -122.4194

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF140707)),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, Color(0xFFE53935).copy(alpha = 0.2f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "🗺️ LIVE GPS LATENCY TARGET",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.Gray
                        )
                        Button(
                            onClick = onNavigateToMap,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E88E5)),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            modifier = Modifier
                                .height(28.dp)
                                .testTag("navigate_button")
                        ) {
                            Text("Full Map", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF1C1C1E))
                            .border(1.dp, Color.DarkGray, RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text("🗺️", fontSize = 32.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Live Location Plotter",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Lat: $lat, Lng: $lng",
                                fontSize = 10.sp,
                                color = Color.LightGray,
                                modifier = Modifier.testTag("gps_coordinates")
                            )
                        }
                    }

                    Text(
                        text = "Nearest Landmark: Approx. 50m north from paired cellular gateway beacon.",
                        fontSize = 11.sp,
                        color = Color.Gray,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // --- RESPONDER LOGS & STATUS DECK ---
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1A0A0A)),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, Color(0xFFE53935).copy(alpha = 0.3f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "🛡️ HELP DESK DISPATCH",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.LightGray
                    )

                    Text(
                        text = emergencySession.responderStatus.uppercase(),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFFFF5252),
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .padding(vertical = 4.dp)
                            .testTag("responder_status_label")
                    )

                    // Helper dispatch action simulator
                    if (emergencySession.isActive && !emergencySession.isAcknowledged) {
                        Text(
                            text = "Waiting for responder or primary contact acknowledgement.",
                            fontSize = 11.sp,
                            color = Color.Gray,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            // --- PRIMARY INTERACTIVE ACTION BUTTONS ---
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Acknowledge Button
                    Button(
                        onClick = { viewModel.acknowledgeEmergency() },
                        enabled = !emergencySession.isAcknowledged,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFFB300),
                            contentColor = Color.Black,
                            disabledContainerColor = Color(0xFF424242),
                            disabledContentColor = Color.Gray
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .testTag("acknowledge_emergency_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.ThumbUp, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (emergencySession.isAcknowledged) "Acknowledged" else "Acknowledge",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }

                    // Call User Button
                    Button(
                        onClick = {
                            val intent = Intent(Intent.ACTION_DIAL).apply {
                                data = Uri.parse("tel:$userPhone")
                            }
                            context.startActivity(intent)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .testTag("call_user_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Call User", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Mute Audio / Alarm
                    Button(
                        onClick = { viewModel.muteEmergencyAlarm() },
                        enabled = !emergencySession.isMuted,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF757575),
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("mute_alarm_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = if (emergencySession.isMuted) Icons.Default.VolumeMute else Icons.Default.VolumeOff,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (emergencySession.isMuted) "Muted" else "Mute Sound", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    // Mark Safe Button
                    Button(
                        onClick = { viewModel.markEmergencySafe() },
                        enabled = !emergencySession.isMarkedSafe,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF00C853),
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("mark_safe_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Mark Safe", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Resolve / End Emergency Button
                Button(
                    onClick = { viewModel.endEmergencySOS("Resolved completely by rescue operators.") },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFD50000),
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .testTag("end_emergency_button"),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Default.Cancel, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("END EMERGENCY", fontWeight = FontWeight.Black, fontSize = 14.sp)
                }
            }
        }
    }
}

@Composable
fun TelemetryRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    valueColor: Color,
    tag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(tag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(16.dp))
            Text(text = label, fontSize = 13.sp, color = Color.White, fontWeight = FontWeight.Medium)
        }
        Text(text = value, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = valueColor)
    }
}
