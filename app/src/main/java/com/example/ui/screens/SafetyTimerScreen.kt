package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.GuardianViewModel
import com.example.service.SafetyTimerStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SafetyTimerScreen(
    viewModel: GuardianViewModel,
    onNavigateBack: () -> Unit
) {
    val service = viewModel.safetyTimerService
    val status by service.status.collectAsState()
    val secondsRemaining by service.secondsRemaining.collectAsState()
    val activityDescription by service.activityDescription.collectAsState()

    var minutesInput by remember { mutableStateOf("5") }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Safety Timer") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (status == SafetyTimerStatus.INACTIVE) {
                OutlinedTextField(
                    value = minutesInput,
                    onValueChange = { minutesInput = it },
                    label = { Text("Minutes") },
                    modifier = Modifier.fillMaxWidth()
                )
                Button(
                    onClick = { service.setTimer(minutesInput.toIntOrNull() ?: 5, "General Activity") },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Start Timer")
                }
            } else {
                Text(
                    text = "Status: ${status.name}",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${secondsRemaining / 60}:${(secondsRemaining % 60).toString().padStart(2, '0')}",
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Black
                )
                Text(text = "Activity: $activityDescription")

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { service.extendTimer(5) }) { Text("+5 Min") }
                    Button(onClick = { service.cancelTimer() }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text("Cancel") }
                    Button(onClick = { service.safeCheckIn() }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))) { Text("Check-In") }
                }
            }
        }
    }
}
