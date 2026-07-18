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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.GuardianViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceSosScreen(
    viewModel: GuardianViewModel,
    onNavigateBack: () -> Unit
) {
    val isListening by viewModel.isVoiceListening.collectAsState()
    val voiceState by viewModel.voiceState.collectAsState()
    val wakePhrases by viewModel.wakePhrases.collectAsState()
    val micDecibels by viewModel.micDecibels.collectAsState()
    val threshold by viewModel.voiceConfidenceThreshold.collectAsState()
    val logs by viewModel.voiceActivationLogs.collectAsState()

    var customPhraseInput by remember { mutableStateOf("") }
    var spokenSimulatedInput by remember { mutableStateOf("Help me") }
    var simulatedConfidence by remember { mutableFloatStateOf(85f) }

    Scaffold(
        topBar = {
            MediumTopAppBar(
                title = {
                    Text(
                        text = "Voice SOS System",
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.testTag("voice_screen_title")
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("voice_screen_back_button")
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
            // --- ACTIVE LISTENING BAR & WAVE ---
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(20.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
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
                                        .size(10.dp)
                                        .background(
                                            if (isListening) Color(0xFF10B981) else Color.Gray,
                                            CircleShape
                                        )
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (isListening) "Mic Active (Offline DSP Loop)" else "Mic Suspended",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                            Switch(
                                checked = isListening,
                                onCheckedChange = { viewModel.voiceSosService.toggleListening(it) },
                                modifier = Modifier.testTag("voice_listening_switch")
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Draw Waveform Oscilloscope based on decibels
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(65.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF0F172A))
                                .padding(8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isListening) {
                                val infiniteTransition = rememberInfiniteTransition(label = "wave")
                                val phase by infiniteTransition.animateFloat(
                                    initialValue = 0f,
                                    targetValue = (2 * Math.PI).toFloat(),
                                    animationSpec = infiniteRepeatable(
                                        animation = tween(1200, easing = LinearEasing),
                                        repeatMode = RepeatMode.Restart
                                    ),
                                    label = "phase_anim"
                                )

                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    val sizeX = size.width
                                    val sizeY = size.height
                                    val centerY = sizeY / 2f
                                    val peak = (micDecibels / 120f) * centerY // Max out wave scale proportional to mic dBs

                                    val points = 60
                                    val step = sizeX / points.toFloat()

                                    for (i in 0 until points - 1) {
                                        val x1 = i * step
                                        val angle1 = (i / points.toFloat()) * (4 * Math.PI) + phase
                                        val y1 = centerY + (Math.sin(angle1) * peak).toFloat()

                                        val x2 = (i + 1) * step
                                        val angle2 = ((i + 1) / points.toFloat()) * (4 * Math.PI) + phase
                                        val y2 = centerY + (Math.sin(angle2) * peak).toFloat()

                                        drawLine(
                                            color = Color(0xFF38BDF8),
                                            start = Offset(x1, y1),
                                            end = Offset(x2, y2),
                                            strokeWidth = 2.dp.toPx()
                                        )
                                    }
                                }
                            } else {
                                Text("Offline Audio System Halted", fontSize = 11.sp, color = Color.Gray)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Acoustic Input level: ${String.format(Locale.US, "%.1f dB", micDecibels)}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("Engine status: $voiceState", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }

            // --- TEST RECOGNITION SIMULATION CARD ---
            item {
                Text(
                    text = "Interactive Speech Matcher",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Say or type a phrase to feed into the offline neural speech classifier. Accidental triggers are automatically rejected below your confidence limit.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(14.dp))

                        // Spoken input choice
                        OutlinedTextField(
                            value = spokenSimulatedInput,
                            onValueChange = { spokenSimulatedInput = it },
                            label = { Text("Spoken Phrase Input") },
                            leadingIcon = { Icon(Icons.Default.Mic, contentDescription = null) },
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth().testTag("voice_input_field")
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Confidence Slider
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Simulated confidence: ${simulatedConfidence.toInt()}%", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text("Filter threshold: $threshold%", fontSize = 11.sp, color = Color.Red, fontWeight = FontWeight.SemiBold)
                        }
                        Slider(
                            value = simulatedConfidence,
                            onValueChange = { simulatedConfidence = it },
                            valueRange = 40f..100f,
                            modifier = Modifier.testTag("confidence_slider")
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Button(
                            onClick = {
                                viewModel.voiceSosService.processVoiceInput(
                                    spokenSimulatedInput,
                                    simulatedConfidence.toInt()
                                )
                            },
                            enabled = isListening,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .testTag("sim_voice_speak_btn")
                        ) {
                            Text("Simulate Vocal Recognition", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // --- ACCIDENTAL FILTER & CONFIG ---
            item {
                Text(
                    text = "Accidental Trigger Shield Configuration",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Set the neural accuracy match threshold to filter ambient chatter and sound slips.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Confidence Guard Limits", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text("$threshold% match", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        }
                        Slider(
                            value = threshold.toFloat(),
                            onValueChange = { viewModel.voiceSosService.setConfidenceThreshold(it.toInt()) },
                            valueRange = 50f..95f,
                            modifier = Modifier.testTag("shield_threshold_slider")
                        )
                    }
                }
            }

            // --- CUSTOM WAKE PHRASES MANAGEMENT ---
            item {
                Text(
                    text = "Custom Emergency Wake Phrases",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = customPhraseInput,
                                onValueChange = { customPhraseInput = it },
                                label = { Text("Add custom wake-word") },
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f).testTag("custom_phrase_input")
                            )
                            Button(
                                onClick = {
                                    if (customPhraseInput.isNotBlank()) {
                                        viewModel.voiceSosService.addWakePhrase(customPhraseInput)
                                        customPhraseInput = ""
                                    }
                                },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.height(54.dp).testTag("add_phrase_btn")
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Add")
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Show current list of words as FlowLayout tags
                        Text("Active trigger words:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(6.dp))

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            wakePhrases.chunked(3).forEach { rowPhrases ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    rowPhrases.forEach { phrase ->
                                        Surface(
                                            color = MaterialTheme.colorScheme.secondaryContainer,
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.testTag("wake_phrase_tag_$phrase")
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(phrase, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSecondaryContainer, fontWeight = FontWeight.SemiBold)
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Icon(
                                                    imageVector = Icons.Default.Close,
                                                    contentDescription = "Delete",
                                                    tint = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.6f),
                                                    modifier = Modifier
                                                        .size(12.dp)
                                                        .clickable { viewModel.voiceSosService.removeWakePhrase(phrase) }
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

            // --- SPEECH DETECTION LOGS ---
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Acoustic Detection Archives",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    TextButton(
                        onClick = { viewModel.voiceSosService.clearLogs() },
                        modifier = Modifier.testTag("clear_voice_logs_btn")
                    ) {
                        Text("Clear Logs", fontSize = 11.sp)
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
            }

            if (logs.isEmpty()) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(modifier = Modifier.padding(18.dp), contentAlignment = Alignment.Center) {
                            Text("No acoustic activations recorded.", fontSize = 11.sp, color = Color.Gray)
                        }
                    }
                }
            } else {
                items(logs) { log ->
                    val dateStr = remember(log.timestampMs) {
                        SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(log.timestampMs))
                    }
                    val accent = if (log.isActivated) Color(0xFFEF4444) else Color(0xFF10B981)

                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().testTag("voice_log_${log.id}")
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(accent.copy(alpha = 0.1f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (log.isActivated) Icons.Default.Warning else Icons.Default.Block,
                                    contentDescription = null,
                                    tint = accent,
                                    modifier = Modifier.size(16.dp)
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
                                        text = "\"${log.phrase}\"",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = dateStr,
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Row {
                                    Text("Confidence: ${log.confidence}% ", fontSize = 10.sp, color = accent, fontWeight = FontWeight.Bold)
                                    Text("• Input level: ${log.noiseFilteredDb.toInt()}dB", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(
                                        text = if (log.isActivated) " • SOS DISPATCHED" else " • BLOCKED",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = accent
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
