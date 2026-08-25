package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.GuardianViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FallDetectionScreen(
    viewModel: GuardianViewModel,
    onNavigateBack: () -> Unit
) {
    val currentState by viewModel.fallState.collectAsState()
    val countdownSeconds by viewModel.fallCountdown.collectAsState()
    val allEvents by viewModel.allFallEvents.collectAsState()
    val mpuReading by viewModel.mpuReading.collectAsState()
    val mpuMotionState by viewModel.mpuMotionState.collectAsState()
    val mpuHardwareState by viewModel.mpuHardwareState.collectAsState()

    // Show Countdown overlay Dialog if in count-down state
    if (currentState == "FALL_COUNTDOWN") {
        FallCountdownDialog(
            secondsLeft = countdownSeconds,
            onCancel = { viewModel.fallDetectionService.cancelFallCountdown() }
        )
    }

    Scaffold(
        topBar = {
            MediumTopAppBar(
                title = {
                    Text(
                        text = "Fall Detection Engine",
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.testTag("fall_screen_title")
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("fall_screen_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.mediumTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            // --- GAIT CLASSIFIER MODULE ---
            item {
                Text(
                    text = "Active Gait & State Classifier",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 8.dp)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    shape = RoundedCornerShape(20.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Big Circular State Visualizer
                        val stateColor = when (currentState) {
                            "WALKING" -> Color(0xFF10B981)
                            "RUNNING" -> Color(0xFF3B82F6)
                            "SITTING" -> Color(0xFFF59E0B)
                            "STANDING" -> Color(0xFF6366F1)
                            "SUDDEN_FALL_DETECTED", "FALL_COUNTDOWN" -> Color(0xFFEF4444)
                            else -> MaterialTheme.colorScheme.primary
                        }

                        val stateIcon = when (currentState) {
                            "WALKING" -> Icons.AutoMirrored.Filled.DirectionsWalk
                            "RUNNING" -> Icons.AutoMirrored.Filled.DirectionsRun
                            "SITTING" -> Icons.Default.Chair
                            "STANDING" -> Icons.Default.AccessibilityNew
                            else -> Icons.Default.ReportProblem
                        }

                        Box(
                            modifier = Modifier
                                .size(110.dp)
                                .clip(CircleShape)
                                .background(stateColor.copy(alpha = 0.12f))
                                .border(2.dp, stateColor.copy(alpha = 0.4f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = stateIcon,
                                contentDescription = null,
                                tint = stateColor,
                                modifier = Modifier.size(54.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = "CURRENT GAIT: $currentState",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            color = stateColor,
                            modifier = Modifier.testTag("current_gait_value")
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "MPU6050 6-axis Gyro & Accelerometer streaming active. Processing neural-net classification vectors locally on device.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )
                    }
                }
            }

            // --- REAL MPU6050 HARDWARE SENSOR TELEMETRY ---
            item {
                Text(
                    text = "ESP32 MPU6050 Live Telemetry",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    shape = RoundedCornerShape(20.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
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
                                "Hardware Motion Pattern",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            val badgeColor = when (mpuMotionState) {
                                com.example.ble.MotionState.POSSIBLE_FALL -> MaterialTheme.colorScheme.error
                                com.example.ble.MotionState.POSSIBLE_IMPACT -> Color(0xFFF59E0B)
                                com.example.ble.MotionState.POSSIBLE_FREE_FALL -> Color(0xFF3B82F6)
                                com.example.ble.MotionState.NORMAL -> Color(0xFF10B981)
                            }
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = badgeColor.copy(alpha = 0.15f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .background(badgeColor, CircleShape)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = mpuMotionState.displayName,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = badgeColor
                                    )
                                }
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Total Magnitude",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            val mag = mpuReading?.accelerationMagnitudeG
                            Text(
                                text = if (mag != null) String.format(Locale.US, "%.2f g", mag) else "Waiting for BLE stream",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = if (mag != null && mag > 2.7) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Acceleration (X/Y/Z)",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = mpuReading?.let {
                                    "${String.format(Locale.US, "%.2f", it.accelerationX)} / ${String.format(Locale.US, "%.2f", it.accelerationY)} / ${String.format(Locale.US, "%.2f", it.accelerationZ)} g"
                                } ?: "--",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Gyroscope (X/Y/Z)",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = mpuReading?.let {
                                    "${String.format(Locale.US, "%.0f", it.gyroX)} / ${String.format(Locale.US, "%.0f", it.gyroY)} / ${String.format(Locale.US, "%.0f", it.gyroZ)} °/s"
                                } ?: "--",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }


            if (allEvents.isEmpty()) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier.padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Database log empty. Fall events recorded by device telemetry will appear here.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                items(allEvents, key = { it.id }) { event ->
                    val dateString = remember(event.timestampMs) {
                        SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(event.timestampMs))
                    }
                    val accent = if (event.eventType.contains("FALL")) Color(0xFFEF4444) else MaterialTheme.colorScheme.primary

                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("fall_log_item_${event.id}")
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(accent.copy(alpha = 0.1f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (event.eventType.contains("FALL")) Icons.Default.Warning else Icons.Default.Check,
                                    contentDescription = null,
                                    tint = accent,
                                    modifier = Modifier.size(18.dp)
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
                                        text = event.eventType,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = accent
                                    )
                                    Text(
                                        text = dateString,
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = event.sensorReadingDetails,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
