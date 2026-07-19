package com.example.ui.screens

import android.content.Context
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AiAnalysisResult
import com.example.model.SensorReading
import com.example.model.TimelineEvent
import com.example.ui.GuardianViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiDashboardScreen(
    viewModel: GuardianViewModel,
    onNavigateBack: () -> Unit
) {
    val aiLogs by viewModel.aiLogs.collectAsState()
    val liveReading by viewModel.currentLiveReading.collectAsState()
    val currentAnalysis by viewModel.currentLiveAnalysis.collectAsState()

    var activeSimulationPattern by remember { mutableStateOf("STILL") } // STILL, WALKING, RUNNING, FALL_DETECTED
    var selectedHistoricalResult by remember { mutableStateOf<AiAnalysisResult?>(null) }

    // Synchronize simulation mode changes to our viewModel service
    LaunchedEffect(activeSimulationPattern) {
        viewModel.aiAnalysisService.startSensorStreamingSimulation(activeSimulationPattern)
    }

    // Safely cleanup the live simulation when leaving this screen
    DisposableEffect(Unit) {
        onDispose {
            viewModel.aiAnalysisService.stopSimulation()
        }
    }

    // Determine what logs to show in the history view (excluding the live-updating active one)
    val displayLogs = remember(aiLogs, currentAnalysis) {
        aiLogs.filter { it.id != currentAnalysis?.id }
    }

    // Display values: prefer selected historical result, else fallback to live analysis
    val activeDisplay = selectedHistoricalResult ?: currentAnalysis ?: AiAnalysisResult(
        id = "default",
        alertId = "none",
        confidenceScore = 0,
        falseAlarmProbability = 0,
        motionPattern = "NO_ACTIVE_EMERGENCY_BROADCAST",
        activityRecognition = "STANDBY MODE (MONITORING)",
        riskLevel = "LOW",
        recommendedAction = "WEARABLE BIND CONNECTED. HARNESS SYSTEM SECURED AND ARMED.",
        sensorData = emptyList(),
        timeline = listOf(
            TimelineEvent("00:00:00", "Sensor Loop Armed", "Waiting for ESP32 broadcast signals...", "🟢")
        )
    )

    Scaffold(
        topBar = {
            MediumTopAppBar(
                title = {
                    Text(
                        text = "AI Analytics Hub",
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.testTag("ai_hub_title")
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("ai_hub_back_button")
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
                // --- SIMULATION CONTROL CONSOLE ---
                item {
                    Text(
                        text = "Interactive Sensor & Gait Simulator",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "Select a physical motion gait below to view the AI classifier algorithm update live telemetry graphs in real-time.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                SimulationModeChip(
                                    label = "Stillness",
                                    icon = "💤",
                                    selected = activeSimulationPattern == "STILL",
                                    onClick = {
                                        selectedHistoricalResult = null
                                        activeSimulationPattern = "STILL"
                                    }
                                )
                                SimulationModeChip(
                                    label = "Walking",
                                    icon = "🚶",
                                    selected = activeSimulationPattern == "WALKING",
                                    onClick = {
                                        selectedHistoricalResult = null
                                        activeSimulationPattern = "WALKING"
                                    }
                                )
                                SimulationModeChip(
                                    label = "Running",
                                    icon = "🏃",
                                    selected = activeSimulationPattern == "RUNNING",
                                    onClick = {
                                        selectedHistoricalResult = null
                                        activeSimulationPattern = "RUNNING"
                                    }
                                )
                                SimulationModeChip(
                                    label = "Fall Event",
                                    icon = "💥",
                                    selected = activeSimulationPattern == "FALL_DETECTED",
                                    onClick = {
                                        selectedHistoricalResult = null
                                        activeSimulationPattern = "FALL_DETECTED"
                                    }
                                )
                            }
                        }
                    }
                }

                // --- CLASSIFIER SUMMARY BOARD ---
                item {
                    val severityColor = when (activeDisplay.riskLevel.uppercase()) {
                        "CRITICAL" -> Color(0xFFD50000)
                        "HIGH" -> Color(0xFFFF8F00)
                        "MEDIUM" -> Color(0xFF1E88E5)
                        else -> Color(0xFF43A047)
                    }

                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                width = 1.5.dp,
                                color = severityColor.copy(alpha = 0.35f),
                                shape = RoundedCornerShape(24.dp)
                            )
                            .testTag("ai_classifier_summary_card")
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(CircleShape)
                                            .background(severityColor)
                                    )
                                    Text(
                                        text = "${activeDisplay.riskLevel} RISK EVENT",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = severityColor
                                    )
                                }

                                if (selectedHistoricalResult != null) {
                                    SuggestionChip(
                                        onClick = { selectedHistoricalResult = null },
                                        label = { Text("Back to Live", fontSize = 10.sp) },
                                        icon = { Icon(Icons.Default.Refresh, null, modifier = Modifier.size(12.dp)) }
                                    )
                                } else {
                                    Text(
                                        text = "📡 LIVE TELEMETRY",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Text(
                                text = activeDisplay.activityRecognition,
                                fontWeight = FontWeight.Black,
                                fontSize = 20.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = "Pattern: ${activeDisplay.motionPattern.replace("_", " ")}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            // Score bars
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "Confidence Score",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = "${activeDisplay.confidenceScore}%",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    LinearProgressIndicator(
                                        progress = { activeDisplay.confidenceScore / 100f },
                                        color = severityColor,
                                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(8.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                    )
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "False Alarm Prob.",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = "${activeDisplay.falseAlarmProbability}%",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    LinearProgressIndicator(
                                        progress = { activeDisplay.falseAlarmProbability / 100f },
                                        color = MaterialTheme.colorScheme.outline,
                                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(8.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(18.dp))

                            // Recommended action card
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = severityColor.copy(alpha = 0.08f)
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Verified,
                                        contentDescription = null,
                                        tint = severityColor,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Column {
                                        Text(
                                            text = "AI RECOMMENDED ACTION",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = severityColor
                                        )
                                        Text(
                                            text = activeDisplay.recommendedAction,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // --- SENSOR TELEMETRY WAVEFORM GRAPHS ---
                item {
                    Text(
                        text = "MPU6050 Raw Live Waveforms",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Showing live Accelerometer (ax, ay, az in Gs) and Gyroscope (gx, gy, gz in deg/s) feeds from physical ESP32 node registers.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            // Graph Label
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(Icons.Default.Analytics, null, tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp))
                                    Text(
                                        text = "3-Axis Accelerometer (Gs)",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    WaveformLegendItem("X", Color(0xFFEF4444))
                                    WaveformLegendItem("Y", Color(0xFF10B981))
                                    WaveformLegendItem("Z", Color(0xFF3B82F6))
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Custom Painted Waveform Canvas
                            SensorWaveformGraph(
                                dataPoints = activeDisplay.sensorData,
                                valueExtractor = { reading -> Triple(reading.ax, reading.ay, reading.az) },
                                minVal = -3f,
                                maxVal = 7f,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(110.dp)
                            )

                            Spacer(modifier = Modifier.height(20.dp))

                            // Gyroscope Label
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(Icons.Default.Speed, null, tint = Color(0xFF8B5CF6), modifier = Modifier.size(16.dp))
                                    Text(
                                        text = "3-Axis Gyroscope (deg/s)",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    WaveformLegendItem("Pitch", Color(0xFF8B5CF6))
                                    WaveformLegendItem("Roll", Color(0xFFEC4899))
                                    WaveformLegendItem("Yaw", Color(0xFFF59E0B))
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            SensorWaveformGraph(
                                dataPoints = activeDisplay.sensorData,
                                valueExtractor = { reading -> Triple(reading.gx / 50f, reading.gy / 50f, reading.gz / 50f) },
                                minVal = -5f,
                                maxVal = 5f,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(110.dp)
                            )
                        }
                    }
                }

                // --- EMERGENCY TIMELINE PANEL ---
                item {
                    Text(
                        text = "AI Sequence Triage Timeline",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            activeDisplay.timeline.forEachIndexed { index, event ->
                                TimelineRow(
                                    event = event,
                                    isLast = index == activeDisplay.timeline.size - 1
                                )
                            }
                        }
                    }
                }

                // --- HISTORICAL LOGS ---
                item {
                    Text(
                        text = "Historical AI Incident Logs",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Tap on any historical incident report synced from Firebase/SQLite databases to inspect its visual waveforms.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (displayLogs.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(100.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No historical incident records registered.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        }
                    }
                } else {
                    items(
                        items = displayLogs,
                        key = { it.id }
                    ) { log ->
                        val itemColor = when (log.riskLevel.uppercase()) {
                            "CRITICAL" -> Color(0xFFD50000)
                            "HIGH" -> Color(0xFFFF8F00)
                            else -> Color(0xFF1E88E5)
                        }

                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (selectedHistoricalResult?.id == log.id) {
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                                } else {
                                    MaterialTheme.colorScheme.surface
                                }
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedHistoricalResult = log }
                                .testTag("historical_log_item_${log.id}")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(itemColor.copy(alpha = 0.12f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = if (log.activityRecognition.contains("FALL")) "💥" else "🔘",
                                        fontSize = 16.sp
                                    )
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = log.activityRecognition,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Risk Level: ${log.riskLevel} | Conf: ${log.confidenceScore}%",
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(log.timestampMs)),
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(log.timestampMs)),
                                        fontSize = 9.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
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
fun SimulationModeChip(
    label: String,
    icon: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
            )
            .border(
                width = 1.dp,
                color = if (selected) Color.Transparent else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                shape = RoundedCornerShape(12.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 8.dp)
            .testTag("simulation_mode_$label"),
        contentAlignment = Alignment.Center
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(icon, fontSize = 11.sp)
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun WaveformLegendItem(label: String, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(
            text = label,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun SensorWaveformGraph(
    dataPoints: List<SensorReading>,
    valueExtractor: (SensorReading) -> Triple<Float, Float, Float>,
    minVal: Float,
    maxVal: Float,
    modifier: Modifier = Modifier
) {
    val axisColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height

        // Draw horizontal grid lines
        val gridCount = 4
        for (i in 0..gridCount) {
            val y = (height / gridCount) * i
            drawLine(
                color = axisColor,
                start = Offset(0f, y),
                end = Offset(width, y),
                strokeWidth = 1f
            )
        }

        if (dataPoints.size < 2) return@Canvas

        val maxPoints = 30
        val pointsToShow = dataPoints.takeLast(maxPoints)
        val stepX = width / (maxPoints - 1)

        val pathX = Path()
        val pathY = Path()
        val pathZ = Path()

        fun mapValueToY(value: Float): Float {
            val clamped = value.coerceIn(minVal, maxVal)
            val percentage = (clamped - minVal) / (maxVal - minVal)
            return height - (percentage * height)
        }

        pointsToShow.forEachIndexed { index, sensorReading ->
            val (valX, valY, valZ) = valueExtractor(sensorReading)
            val posX = index * stepX
            val posY_X = mapValueToY(valX)
            val posY_Y = mapValueToY(valY)
            val posY_Z = mapValueToY(valZ)

            if (index == 0) {
                pathX.moveTo(posX, posY_X)
                pathY.moveTo(posX, posY_Y)
                pathZ.moveTo(posX, posY_Z)
            } else {
                pathX.lineTo(posX, posY_X)
                pathY.lineTo(posX, posY_Y)
                pathZ.lineTo(posX, posY_Z)
            }
        }

        // Draw the waveforms with elegant color strokes
        drawPath(
            path = pathX,
            color = Color(0xFFEF4444),
            style = Stroke(width = 2.dp.toPx())
        )
        drawPath(
            path = pathY,
            color = Color(0xFF10B981),
            style = Stroke(width = 2.dp.toPx())
        )
        drawPath(
            path = pathZ,
            color = Color(0xFF3B82F6),
            style = Stroke(width = 2.dp.toPx())
        )
    }
}

@Composable
fun TimelineRow(
    event: TimelineEvent,
    isLast: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(event.iconEmoji, fontSize = 14.sp)
            }

            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(38.dp)
                        .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f))
                )
            }
        }

        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = event.event,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = event.time,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = event.description,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
