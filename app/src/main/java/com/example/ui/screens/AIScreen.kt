package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AIAnalysisModel
import com.example.model.AISensorReading
import com.example.model.AITimelineEvent
import com.example.ui.GuardianViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AIScreen(
    viewModel: GuardianViewModel,
    onNavigateBack: () -> Unit
) {
    val aiLogs by viewModel.aiLogsNew.collectAsState()
    val liveReading by viewModel.currentLiveReadingNew.collectAsState()
    val currentAnalysis by viewModel.currentLiveAnalysisNew.collectAsState()

    var activeSimulationPattern by remember { mutableStateOf("STILL") }
    var selectedHistoricalResult by remember { mutableStateOf<AIAnalysisModel?>(null) }

    LaunchedEffect(activeSimulationPattern) {
        viewModel.aiProvider.startSimulation(activeSimulationPattern)
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.aiProvider.stopSimulation()
        }
    }

    val displayLogs = remember(aiLogs, currentAnalysis) {
        aiLogs.filter { it.id != currentAnalysis?.id }
    }

    val activeDisplay = selectedHistoricalResult ?: currentAnalysis ?: AIAnalysisModel(
        id = "default",
        alertId = "none",
        confidenceScore = 0,
        falseAlarmProbability = 0,
        motionAnalysis = "STANDBY_MONITORING_GAIT_NORMAL",
        activityRecognition = "STANDBY MODE (MONITORING)",
        riskLevel = "LOW",
        suggestedAction = "WEARABLE DISPATCH INTEGRATED. SYSTEM SECURED AND ARMED.",
        sensorReadings = emptyList(),
        timeline = listOf(
            AITimelineEvent("00:00:00", "Sensor Loop Armed", "Waiting for ESP32 broadcast signals...", "🟢")
        )
    )

    Scaffold(
        topBar = {
            MediumTopAppBar(
                title = {
                    Text(
                        text = "AI Emergency Analytics",
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.testTag("ai_screen_title")
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("ai_screen_back_button")
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 32.dp)
            ) {
                // --- GAIT MOTION SELECTOR ---
                item {
                    Text(
                        text = "Real-Time Sensor Gait & Fall Simulator",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "Simulate accelerometer & gyro vectors to test our advanced gait classifier and automated neural fallback alerts.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf("STILL", "WALKING", "RUNNING", "FALL_DETECTED").forEach { pat ->
                                    val isSelected = activeSimulationPattern == pat
                                    val btnColor = if (pat == "FALL_DETECTED") Color(0xFFD50000) else MaterialTheme.colorScheme.primary
                                    Button(
                                        onClick = {
                                            selectedHistoricalResult = null
                                            activeSimulationPattern = pat
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (isSelected) btnColor else MaterialTheme.colorScheme.surface,
                                            contentColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                                        ),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(38.dp)
                                            .testTag("sim_pat_$pat"),
                                        contentPadding = PaddingValues(0.dp)
                                    ) {
                                        Text(
                                            text = when(pat) {
                                                "FALL_DETECTED" -> "FALL"
                                                else -> pat
                                            },
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // --- CLASSIFICATION TELEMETRY HEADER ---
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = if (selectedHistoricalResult != null) "Archived Incident Report" else "Live Classifier Telemetry",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        if (selectedHistoricalResult != null) {
                            TextButton(
                                onClick = { selectedHistoricalResult = null },
                                modifier = Modifier.testTag("back_to_live_btn")
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.LiveTv, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Back to Live", fontSize = 11.sp)
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))

                    // --- MAIN METRICS CARD ---
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        shape = RoundedCornerShape(20.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                // Confidence Gauge
                                Box(
                                    modifier = Modifier
                                        .size(100.dp)
                                        .weight(0.4f),
                                    contentAlignment = Alignment.Center
                                ) {
                                    val angle = (activeDisplay.confidenceScore / 100f) * 360f
                                    val animatedAngle by animateFloatAsState(targetValue = angle, label = "gauge_anim")
                                    val accentColor = when (activeDisplay.riskLevel) {
                                        "CRITICAL" -> Color(0xFFD50000)
                                        "HIGH" -> Color(0xFFFF8F00)
                                        else -> MaterialTheme.colorScheme.primary
                                    }

                                    Canvas(modifier = Modifier.fillMaxSize()) {
                                        drawCircle(
                                            color = accentColor.copy(alpha = 0.12f),
                                            style = Stroke(width = 12.dp.toPx())
                                        )
                                        drawArc(
                                            color = accentColor,
                                            startAngle = -90f,
                                            sweepAngle = animatedAngle,
                                            useCenter = false,
                                            style = Stroke(width = 12.dp.toPx())
                                        )
                                    }
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = "${activeDisplay.confidenceScore}%",
                                            fontSize = 20.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = accentColor,
                                            modifier = Modifier.testTag("confidence_score_val")
                                        )
                                        Text(
                                            text = "CONFIDENCE",
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                // Secondary Info Bar
                                Column(
                                    modifier = Modifier.weight(0.6f),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    // Activity Recognition Title
                                    Column {
                                        Text(
                                            text = "Activity Recognition",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = activeDisplay.activityRecognition,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.testTag("activity_recognition_val")
                                        )
                                    }

                                    // Risk level and False alarm prob
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                text = "Risk Level",
                                                fontSize = 10.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            val riskColor = when (activeDisplay.riskLevel) {
                                                "CRITICAL" -> Color(0xFFD50000)
                                                "HIGH" -> Color(0xFFFF8F00)
                                                else -> Color(0xFF43A047)
                                            }
                                            Surface(
                                                color = riskColor.copy(alpha = 0.15f),
                                                shape = RoundedCornerShape(6.dp)
                                            ) {
                                                Text(
                                                    text = activeDisplay.riskLevel,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = riskColor,
                                                    modifier = Modifier
                                                        .padding(horizontal = 6.dp, vertical = 3.dp)
                                                        .testTag("risk_level_val")
                                                )
                                            }
                                        }

                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(
                                                text = "False Alarm Prob",
                                                fontSize = 10.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                text = "${activeDisplay.falseAlarmProbability}%",
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.error,
                                                modifier = Modifier.testTag("false_alarm_val")
                                            )
                                        }
                                    }
                                }
                            }

                            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                            // Motion analysis field
                            Row(verticalAlignment = Alignment.Top) {
                                Icon(Icons.Default.Waves, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text("Gait Motion Analysis Pattern", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(
                                        text = activeDisplay.motionAnalysis,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.testTag("motion_analysis_val")
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Suggested Action card
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                                    .padding(10.dp)
                            ) {
                                Row(verticalAlignment = Alignment.Top) {
                                    Icon(
                                        imageVector = Icons.Default.HealthAndSafety,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text("AI Suggested Resolution Protocol", fontSize = 10.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = activeDisplay.suggestedAction,
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.testTag("suggested_action_val")
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // --- SENSOR GRAPH OSCILLOSCOPE ---
                item {
                    Text(
                        text = "MPU6050 Accelerometer Plot (G)",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(130.dp)
                                    .background(Color(0xFF0F172A), shape = RoundedCornerShape(8.dp))
                                    .border(1.dp, Color(0xFF334155), shape = RoundedCornerShape(8.dp))
                                    .padding(8.dp)
                            ) {
                                val readings = activeDisplay.sensorReadings
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    if (readings.isEmpty()) return@Canvas
                                    val sizeX = size.width
                                    val sizeY = size.height
                                    val centerY = sizeY / 2f
                                    val step = sizeX / 20f

                                    val pathX = Path()
                                    val pathY = Path()
                                    val pathZ = Path()

                                    readings.forEachIndexed { index, r ->
                                        val curX = index * step
                                        // Max acceleration mapped to 4.0G, minimum -4.0G
                                        val valX = centerY - (r.ax / 4.0f) * centerY
                                        val valY = centerY - (r.ay / 4.0f) * centerY
                                        val valZ = centerY - (r.az / 4.0f) * centerY

                                        if (index == 0) {
                                            pathX.moveTo(curX, valX)
                                            pathY.moveTo(curX, valY)
                                            pathZ.moveTo(curX, valZ)
                                        } else {
                                            pathX.lineTo(curX, valX)
                                            pathY.lineTo(curX, valY)
                                            pathZ.lineTo(curX, valZ)
                                        }
                                    }

                                    drawPath(pathX, Color(0xFFEF4444), style = Stroke(width = 2.dp.toPx()))
                                    drawPath(pathY, Color(0xFF10B981), style = Stroke(width = 2.dp.toPx()))
                                    drawPath(pathZ, Color(0xFF3B82F6), style = Stroke(width = 2.dp.toPx()))
                                }
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceAround
                            ) {
                                LegendItem(color = Color(0xFFEF4444), label = "X-Accel (ax): ${String.format(Locale.US, "%.2fG", liveReading.ax)}")
                                LegendItem(color = Color(0xFF10B981), label = "Y-Accel (ay): ${String.format(Locale.US, "%.2fG", liveReading.ay)}")
                                LegendItem(color = Color(0xFF3B82F6), label = "Z-Accel (az): ${String.format(Locale.US, "%.2fG", liveReading.az)}")
                            }
                        }
                    }
                }

                // --- EMERGENCY TIMELINE ---
                item {
                    Text(
                        text = "Incident Neural Timeline logs",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            if (activeDisplay.timeline.isEmpty()) {
                                Text("No neural timeline events logged.", fontSize = 12.sp, color = Color.Gray)
                            } else {
                                activeDisplay.timeline.forEachIndexed { index, event ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Box(
                                                modifier = Modifier
                                                    .size(28.dp)
                                                    .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(event.categoryEmoji, fontSize = 12.sp)
                                            }
                                            if (index < activeDisplay.timeline.size - 1) {
                                                Box(
                                                    modifier = Modifier
                                                        .width(2.dp)
                                                        .height(28.dp)
                                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column(modifier = Modifier.padding(bottom = 12.dp)) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Text(
                                                    text = event.eventName,
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                Text(
                                                    text = event.timeString,
                                                    fontSize = 10.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = event.eventDescription,
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

                // --- ARCHIVED EVENTS LIST ---
                item {
                    Text(
                        text = "Saved AI Classification Archives",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }

                if (displayLogs.isEmpty()) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(modifier = Modifier.padding(16.dp), contentAlignment = Alignment.Center) {
                                Text(
                                    text = "No saved historical logs found.",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                } else {
                    items(displayLogs) { log ->
                        val dateString = remember(log.timestampMs) {
                            SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(log.timestampMs))
                        }
                        val cardBorderColor = if (selectedHistoricalResult?.id == log.id) MaterialTheme.colorScheme.primary else Color.Transparent

                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.5.dp, cardBorderColor, RoundedCornerShape(12.dp))
                                .clickable {
                                    selectedHistoricalResult = if (selectedHistoricalResult?.id == log.id) null else log
                                }
                                .testTag("archived_log_${log.id}")
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(0.7f)) {
                                    Text(
                                        text = log.activityRecognition,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "$dateString • Risk: ${log.riskLevel}",
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Row(
                                    modifier = Modifier.weight(0.3f),
                                    horizontalArrangement = Arrangement.End,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${log.confidenceScore}%",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Black,
                                        color = when (log.riskLevel) {
                                            "CRITICAL" -> Color(0xFFD50000)
                                            "HIGH" -> Color(0xFFFF8F00)
                                            else -> MaterialTheme.colorScheme.primary
                                        }
                                    )
                                    Icon(
                                        imageVector = Icons.Default.ChevronRight,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(color, CircleShape)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = label, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
    }
}
