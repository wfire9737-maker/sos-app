package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.service.AuthState
import com.example.ui.GuardianViewModel
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.absoluteValue
import kotlin.math.cos
import kotlin.math.sin

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

    var showPinDialog by remember { mutableStateOf(false) }
    var pinText by remember { mutableStateOf("") }

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
        String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
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
    val currentUser = (authState as? AuthState.Success)?.user
    val userName = emergencySession.activeAlert?.userName ?: currentUser?.name ?: "Marcus Vance"
    val userPhone = emergencySession.activeAlert?.userPhone ?: currentUser?.phone ?: "+1-555-0143"
    val medicalInfo = currentUser?.medicalInfo ?: "Type-1 Diabetes, High blood pressure, penicillin allergy"

    // Primary coordinates
    val lat = emergencySession.activeAlert?.latitude ?: 37.7749
    val lng = emergencySession.activeAlert?.longitude ?: -122.4194

    // Dynamic address reverse geocoder simulation
    val currentAddress = remember(lat, lng) {
        getMockAddress(lat, lng)
    }

    // Unsplash user avatar fallback for a beautiful profile presentation
    val userPhotoUri = currentUser?.photoUri ?: "https://images.unsplash.com/photo-1534528741775-53994a69daeb?q=80&w=256&auto=format&fit=crop"

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
                            color = Color.White
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
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color(0xFF8B0000).copy(alpha = pulsingAlpha),
                    titleContentColor = Color.White
                )
            )
        },
        containerColor = Color(0xFF0F0404) // Ultra dark danger warning theme
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
                        Text("EMERGENCY TIMER", fontSize = 10.sp, color = Color.LightGray, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
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
                        Text("EMERGENCY LEVEL", fontSize = 10.sp, color = Color.LightGray, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
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
                    // Profile Photo (using Coil load or styled initials fallback)
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .border(2.dp, Color(0xFFFF5252), CircleShape)
                    ) {
                        AsyncImage(
                            model = userPhotoUri,
                            contentDescription = "User Photo",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
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
                            text = "EMG ID: ${emergencySession.activeAlert?.id ?: "EMG-ACTIVE-81F4"}",
                            fontSize = 12.sp,
                            color = Color.LightGray,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.testTag("emergency_id_text")
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Medical Profile: $medicalInfo",
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
                        text = "🛰️ REAL-TIME SYSTEM TELEMETRY DECK",
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
                            Text("AI Confidence Score", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                        Text(
                            text = "${emergencySession.aiConfidence}% ACCURACY",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF66BB6A),
                            modifier = Modifier.testTag("ai_confidence_score")
                        )
                    }

                    // Battery status
                    TelemetryRow(
                        icon = Icons.Default.BatteryAlert,
                        label = "Device Battery",
                        value = "${emergencySession.batteryLevel}% remaining",
                        valueColor = if (emergencySession.batteryLevel > 30) Color.Green else Color.Red,
                        tag = "battery_status"
                    )

                    // GPS Status
                    TelemetryRow(
                        icon = Icons.Default.GpsFixed,
                        label = "GPS Fix Quality",
                        value = emergencySession.gpsStatus,
                        valueColor = Color(0xFF29B6F6),
                        tag = "gps_status"
                    )

                    // WiFi Status (Requested explicitly)
                    TelemetryRow(
                        icon = Icons.Default.Wifi,
                        label = "WiFi Connection Status",
                        value = "ESP-SOS-WIFI-MESH (CONNECTED)",
                        valueColor = Color(0xFF00E676),
                        tag = "wifi_status"
                    )

                    // Internet status
                    TelemetryRow(
                        icon = Icons.Default.NetworkCheck,
                        label = "Internet Link Quality",
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
                }
            }

            // --- GEOLOCATION ADDRESS & GOOGLE MAP ---
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF140707)),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, Color(0xFFE53935).copy(alpha = 0.2f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "🗺️ RESPONDER GEOLOCATION TARGET",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.Gray
                    )

                    HorizontalDivider(color = Color(0xFFE53935).copy(alpha = 0.2f))

                    // Address Display
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF220D0D), RoundedCornerShape(12.dp))
                            .border(1.dp, Color(0xFFD50000).copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color(0xFFFF5252))
                            Text(
                                text = "Current Resolved Address:",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.LightGray
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = currentAddress,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.testTag("current_address_text")
                        )
                    }

                    // Tactical Google Map Canvas Representation
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .border(1.dp, Color.Gray.copy(alpha = 0.3f), RoundedCornerShape(14.dp))
                    ) {
                        TacticalEmergencyMap(
                            userLat = lat,
                            userLng = lng,
                            modifier = Modifier.fillMaxSize()
                        )

                        // Top right quick link to standard map screen
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(8.dp)
                                .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(8.dp))
                                .clickable { onNavigateToMap() }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text("Full Map ↗", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // --- RESPONDER LOGS & DISPATCH STATUS ---
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
                        text = "🛡️ RESPONDER RESPONSE DESK",
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
                }
            }

            // --- PRIMARY INTERACTIVE ACTION BUTTON CONTROL DECK ---
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // ROW 1: Call Emergency Contact & Share Location
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Call Emergency Contact
                    Button(
                        onClick = {
                            val contactPhone = currentUser?.emergencyContactPhone ?: "+1-555-0199"
                            val intent = Intent(Intent.ACTION_DIAL).apply {
                                data = Uri.parse("tel:$contactPhone")
                            }
                            context.startActivity(intent)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E88E5)),
                        modifier = Modifier
                            .weight(1f)
                            .height(54.dp)
                            .testTag("call_contact_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.ContactPhone, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (!currentUser?.emergencyContactName.isNullOrBlank()) {
                                "Call ${currentUser?.emergencyContactName}"
                            } else {
                                "Call Contact"
                            },
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }

                    // Share Location
                    Button(
                        onClick = {
                            val shareBody = "⚠️ EMERGENCY SOS ACTIVE - PAIRING ID: ${emergencySession.activeAlert?.id ?: "SOS-BAND"}\n" +
                                    "Resolved Address: $currentAddress\n" +
                                    "Google Maps Live Beacon: https://maps.google.com/?q=$lat,$lng"
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, shareBody)
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Share Location Coordinates"))
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7B1FA2)),
                        modifier = Modifier
                            .weight(1f)
                            .height(54.dp)
                            .testTag("share_location_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Share Loc", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }

                // ROW 2: Navigate To coordinates & Mark Safe
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Navigate to Emergency coordinates
                    Button(
                        onClick = {
                            val gmmIntentUri = Uri.parse("geo:$lat,$lng?q=$lat,$lng(Emergency Location)")
                            val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri).apply {
                                setPackage("com.google.android.apps.maps")
                            }
                            context.startActivity(mapIntent)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF388E3C)),
                        modifier = Modifier
                            .weight(1f)
                            .height(54.dp)
                            .testTag("navigate_to_emergency_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Directions, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Navigate", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }

                    // Mark Safe
                    Button(
                        onClick = { viewModel.markEmergencySafe() },
                        enabled = !emergencySession.isMarkedSafe,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF00C853),
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(54.dp)
                            .testTag("mark_safe_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Mark Safe", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }

                // PIN Verification Cancel dialog triggers
                if (showPinDialog) {
                    AlertDialog(
                        onDismissRequest = { showPinDialog = false },
                        title = {
                            Text(
                                "Enter Security PIN",
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        },
                        text = {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    "Enter your secure 4-digit PIN to cancel this active emergency session.",
                                    color = Color.LightGray,
                                    fontSize = 14.sp
                                )
                                OutlinedTextField(
                                    value = pinText,
                                    onValueChange = { if (it.length <= 4) pinText = it },
                                    label = { Text("4-Digit PIN") },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedLabelColor = Color(0xFFFF5252),
                                        unfocusedLabelColor = Color.Gray,
                                        focusedBorderColor = Color(0xFFFF5252),
                                        unfocusedBorderColor = Color.Gray
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("pin_entry_input")
                                )
                            }
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    viewModel.cancelEmergencyWithPin(pinText) { success ->
                                        if (success) {
                                            showPinDialog = false
                                            pinText = ""
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD50000)),
                                modifier = Modifier.testTag("submit_pin_button")
                            ) {
                                Text("Verify & Cancel")
                            }
                        },
                        dismissButton = {
                            TextButton(
                                onClick = { showPinDialog = false; pinText = "" }
                            ) {
                                Text("Dismiss", color = Color.Gray)
                            }
                        },
                        containerColor = Color(0xFF160808),
                        shape = RoundedCornerShape(16.dp)
                    )
                }

                // ROW 3: PIN Cancel & End Emergency Override
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Secure PIN Cancel Button
                    Button(
                        onClick = { showPinDialog = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFE53935),
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .testTag("pin_cancel_emergency_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.LockOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("PIN Cancel", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    // Dispatcher End Emergency Button
                    Button(
                        onClick = { viewModel.endEmergencySOS("Resolved completely by rescue operators.") },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF555555),
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .testTag("end_emergency_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Cancel, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Force End", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun TacticalEmergencyMap(
    userLat: Double,
    userLng: Double,
    modifier: Modifier = Modifier
) {
    // Dynamic looping for pulse effects
    val timeMs = System.currentTimeMillis()
    val pulseRatio = (timeMs % 2000) / 2000f

    Canvas(modifier = modifier.background(Color(0xFF0F0B0B))) {
        val width = size.width
        val height = size.height
        val cx = width / 2f
        val cy = height / 2f

        // 1. Draw Tactical GPS coordinate gridlines
        val gridSpacing = 40.dp.toPx()
        val cols = (width / gridSpacing).toInt() + 2
        val rows = (height / gridSpacing).toInt() + 2
        for (i in -1..cols) {
            val lx = i * gridSpacing
            drawLine(
                color = Color(0xFF2E1515),
                start = Offset(lx, 0f),
                end = Offset(lx, height),
                strokeWidth = 1f
            )
        }
        for (i in -1..rows) {
            val ly = i * gridSpacing
            drawLine(
                color = Color(0xFF2E1515),
                start = Offset(0f, ly),
                end = Offset(width, ly),
                strokeWidth = 1f
            )
        }

        // 2. Draw mock topographical river stream
        val riverPath = Path().apply {
            moveTo(0f, height * 0.45f)
            cubicTo(width * 0.3f, height * 0.38f, width * 0.7f, height * 0.68f, width, height * 0.62f)
        }
        drawPath(
            path = riverPath,
            color = Color(0xFF14243E).copy(alpha = 0.5f),
            style = Stroke(width = 8.dp.toPx())
        )

        // 3. Draw mock street grids
        drawLine(color = Color(0xFF2A1C1C), start = Offset(0f, cy * 0.8f), end = Offset(width, cy * 1.2f), strokeWidth = 14f)
        drawLine(color = Color(0xFF2A1C1C), start = Offset(cx * 1.1f, 0f), end = Offset(cx * 0.9f, height), strokeWidth = 14f)

        // 4. Concentric warning broadcast waves / Radar lines
        drawCircle(
            color = Color(0xFFFF5252).copy(alpha = 0.2f * (1f - pulseRatio)),
            radius = 110.dp.toPx() * pulseRatio,
            center = Offset(cx, cy)
        )
        drawCircle(
            color = Color(0xFFFF5252).copy(alpha = 0.15f),
            radius = 40.dp.toPx(),
            center = Offset(cx, cy),
            style = Stroke(width = 1f)
        )
        drawCircle(
            color = Color(0xFFFF5252).copy(alpha = 0.08f),
            radius = 80.dp.toPx(),
            center = Offset(cx, cy),
            style = Stroke(width = 1f)
        )

        // Radar sweep arm simulation
        val sweepAngleRad = Math.toRadians((timeMs % 4000) / 4000.0 * 360.0)
        val sweepLen = 140.dp.toPx()
        drawLine(
            color = Color(0xFFFF3D00).copy(alpha = 0.25f),
            start = Offset(cx, cy),
            end = Offset(cx + sweepLen * cos(sweepAngleRad).toFloat(), cy + sweepLen * sin(sweepAngleRad).toFloat()),
            strokeWidth = 2.dp.toPx()
        )

        // 5. User live location target pointer / Marker core
        drawCircle(color = Color.White, radius = 9.dp.toPx(), center = Offset(cx, cy))
        drawCircle(color = Color(0xFFFF1744), radius = 6.dp.toPx(), center = Offset(cx, cy))
    }
}

// Deterministic Address Generator
fun getMockAddress(lat: Double, lng: Double): String {
    val hash = (lat * 10000 + lng * 10000).absoluteValue.toInt()
    val streetNum = (hash % 1450) + 101
    val streets = listOf("Constitution Ave", "Market St", "Mission St", "Broadway Blvd", "Pine Rd", "El Camino Real", "University Ave", "Shattuck Ave", "Geary Blvd")
    val cities = listOf("San Francisco", "Oakland", "San Jose", "Berkeley", "Palo Alto")
    val street = streets[hash % streets.size]
    val city = cities[(hash / streets.size) % cities.size]
    return "$streetNum $street, $city, CA ${94000 + (hash % 999)}"
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
            Spacer(modifier = Modifier.width(4.dp))
            Text(text = label, fontSize = 13.sp, color = Color.White, fontWeight = FontWeight.Medium)
        }
        Text(text = value, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = valueColor)
    }
}

