import os

content = """package com.example.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.ui.GuardianViewModel
import com.example.ui.rememberLocationPermissionHandler
import kotlin.math.roundToInt
import com.example.model.SosWorkflowState

// Stitch UI Colors
private val StitchBg = Color(0xFF0F1115)
private val StitchCard = Color(0xFF1A1C23)
private val StitchRed = Color(0xFFE5534B)
private val StitchGreen = Color(0xFF20E070)
private val StitchPurple = Color(0xFF6A6CFF)
private val StitchTextMuted = Color(0xFFA0A0A5)

@Composable
fun EmergencyScreen(viewModel: GuardianViewModel) {
    val context = LocalContext.current
    val emergencySession by viewModel.emergencySession.collectAsState()
    val countdown by viewModel.countdown.collectAsState()
    val sosWorkflowState by viewModel.sosWorkflowState.collectAsState()
    
    var showPinDialog by remember { mutableStateOf(false) }

    // Background with Red Glow at bottom
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(StitchBg)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(400.dp)
                .align(Alignment.BottomCenter)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color.Transparent, StitchRed.copy(alpha = 0.2f), StitchRed.copy(alpha = 0.4f))
                    )
                )
        )
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            
            // Faint SOS background circle
            Box(contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier
                        .size(300.dp)
                        .clip(CircleShape)
                        .border(2.dp, StitchRed.copy(alpha = 0.1f), CircleShape)
                )
                Box(
                    modifier = Modifier
                        .size(200.dp)
                        .clip(CircleShape)
                        .border(1.dp, StitchRed.copy(alpha = 0.2f), CircleShape)
                )
                
                if (countdown != null) {
                    Text(
                        text = countdown.toString(),
                        color = StitchRed,
                        fontSize = 72.sp,
                        fontWeight = FontWeight.Black
                    )
                } else {
                    StitchEmergencyCard()
                }
            }
            
            Spacer(modifier = Modifier.height(48.dp))
            
            // Slide to cancel
            StitchSlideToCancel {
                if (countdown != null) {
                    viewModel.cancelEmergencyWithPin("") {}
                } else {
                    showPinDialog = true
                }
            }
        }
    }
    
    if (showPinDialog) {
        CancelSosDialog(
            onDismiss = { showPinDialog = false },
            onConfirm = { pin ->
                viewModel.cancelEmergencyWithPin(pin) { success ->
                    if (success) {
                        showPinDialog = false
                    }
                }
            }
        )
    }
}

@Composable
fun StitchEmergencyCard() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(StitchCard.copy(alpha = 0.8f)) // slight transparency to show glow
            .padding(24.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "SOS INITIATED",
                    color = StitchRed.copy(alpha = 0.5f), // It's faint in the design
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 2.sp
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(StitchGreen))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Live", color = StitchGreen, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Row(verticalAlignment = Alignment.Top) {
                // Map Thumbnail mock
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF2A2A35)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.LocationOn, contentDescription = "Location", tint = StitchRed)
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                // Status list
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Status", color = StitchTextMuted, fontSize = 12.sp)
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = StitchGreen, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("SMS Dispatched", color = Color.White, fontSize = 14.sp)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Mic, contentDescription = null, tint = StitchPurple, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Audio Recording...", color = Color.White, fontSize = 14.sp)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Spacer(modifier = Modifier.width(24.dp))
                        Text("Emergency Services Pending", color = StitchTextMuted, fontSize = 14.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun StitchSlideToCancel(onCancelled: () -> Unit) {
    val slideMax = with(LocalDensity.current) { 220.dp.toPx() }
    var offsetX by remember { mutableFloatStateOf(0f) }
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .clip(RoundedCornerShape(32.dp))
            .background(StitchCard),
        contentAlignment = Alignment.CenterStart
    ) {
        // Text behind
        Text(
            text = "SLIDE TO CANCEL",
            color = StitchTextMuted,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            modifier = Modifier.align(Alignment.Center)
        )
        
        // Draggable nub
        Box(
            modifier = Modifier
                .offset { IntOffset(offsetX.roundToInt(), 0) }
                .size(64.dp)
                .clip(CircleShape)
                .background(Color(0xFF2A2A35))
                .draggable(
                    orientation = Orientation.Horizontal,
                    state = rememberDraggableState { delta ->
                        offsetX = (offsetX + delta).coerceIn(0f, slideMax)
                    },
                    onDragStopped = {
                        if (offsetX > slideMax * 0.8f) {
                            offsetX = slideMax
                            onCancelled()
                            offsetX = 0f
                        } else {
                            offsetX = 0f
                        }
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Close, contentDescription = "Cancel", tint = Color.White)
        }
    }
}

@Composable
fun CancelSosDialog(onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var pin by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Cancel Emergency", fontWeight = FontWeight.Bold, color = Color.White) },
        text = {
            Column {
                Text("Enter your 4-digit PIN to cancel.", color = StitchTextMuted)
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = pin,
                    onValueChange = { if (it.length <= 4) pin = it },
                    label = { Text("PIN") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        containerColor = StitchCard,
        confirmButton = {
            Button(
                onClick = { onConfirm(pin) },
                enabled = pin.length == 4,
                colors = ButtonDefaults.buttonColors(containerColor = StitchRed)
            ) { Text("Confirm", color = Color.White) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Back", color = StitchTextMuted) } }
    )
}
"""

with open("app/src/main/java/com/example/ui/screens/EmergencyScreen.kt", "w") as f:
    f.write(content)
