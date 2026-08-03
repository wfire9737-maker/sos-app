package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.example.ui.GuardianViewModel
import com.example.ui.theme.*

import com.example.ui.rememberLocationPermissionHandler
import com.example.model.SosWorkflowState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmergencyScreen(
    viewModel: GuardianViewModel,
    onNavigateBack: () -> Unit
) {
    val emergencySession by viewModel.emergencySession.collectAsState()
    val isSirenPlaying by viewModel.isSirenPlaying.collectAsState()
    val sosSoundEnabled by viewModel.sosSoundEnabled.collectAsState()
    val countdown by viewModel.countdown.collectAsState()
    val contacts by viewModel.contacts.collectAsState()
    val sosWorkflowState by viewModel.sosWorkflowState.collectAsState()
    val context = LocalContext.current
    val primaryContactPhone = contacts.firstOrNull()?.phone ?: "911"
    
    val sosTriggerHandler = rememberLocationPermissionHandler {
        viewModel.triggerManualSOS()
    }
    
    if (sosWorkflowState != SosWorkflowState.IDLE) {
        androidx.compose.ui.window.Dialog(onDismissRequest = {}) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier.padding(24.dp).fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = when (sosWorkflowState) {
                            SosWorkflowState.OBTAINING_LOCATION -> "Obtaining high accuracy location..."
                            SosWorkflowState.SENDING_SMS -> "Sending SMS to emergency contacts..."
                            SosWorkflowState.CALLING_CONTACT -> "Placing emergency call..."
                            SosWorkflowState.UPLOADING -> "Uploading SOS alert to servers..."
                            SosWorkflowState.COMPLETED -> "SOS Completed!"
                            else -> "Preparing SOS..."
                        },
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
    
    val callPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            val intent = Intent(Intent.ACTION_CALL).apply { data = Uri.parse("tel:$primaryContactPhone") }
            context.startActivity(intent)
        }
    }
    
    // Status text animation
    var flashWarning by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        while(true) {
            flashWarning = !flashWarning
            delay(800)
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("ACTIVE EMERGENCY", fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.error) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Hero section with pulsing background
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(if (flashWarning) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.error)
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (countdown != null) {
                        Text(
                            text = countdown.toString(),
                            fontSize = 72.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onError
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "SENDING SOS...",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onError
                        )
                        Text(
                            text = "Tap CANCEL to abort.",
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onError.copy(alpha=0.8f)
                        )
                    } else {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = "Warning",
                            modifier = Modifier.size(64.dp),
                            tint = if (flashWarning) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onError
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "SOS ACTIVATED",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black,
                            color = if (flashWarning) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onError
                        )
                        Text(
                            text = "Help is on the way. Stay calm.",
                            fontSize = 16.sp,
                            color = if (flashWarning) MaterialTheme.colorScheme.error.copy(alpha=0.8f) else MaterialTheme.colorScheme.onError.copy(alpha=0.8f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Action Cards
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                ActionCard(
                    title = "Call Emergency Contact",
                    subtitle = "Instantly dials the primary emergency contact or 911",
                    icon = Icons.Default.Phone,
                    color = MaterialTheme.colorScheme.error,
                    onClick = {
                        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED) {
                            val intent = Intent(Intent.ACTION_CALL).apply { data = Uri.parse("tel:$primaryContactPhone") }
                            context.startActivity(intent)
                        } else {
                            callPermissionLauncher.launch(Manifest.permission.CALL_PHONE)
                        }
                    }
                )
                
                ActionCard(
                    title = "Notify Contacts Again",
                    subtitle = "Resends SOS SMS with your live location",
                    icon = Icons.Default.Sms,
                    color = AlertOrange,
                    onClick = {
                        val message = "🚨 EMERGENCY SOS: I need help! Location: https://maps.google.com/?q=${emergencySession?.activeAlert?.latitude ?: 0.0},${emergencySession?.activeAlert?.longitude ?: 0.0}"
                        val intent = Intent(Intent.ACTION_SENDTO).apply {
                            data = Uri.parse("smsto:$primaryContactPhone")
                            putExtra("sms_body", message)
                        }
                        try {
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            // Fallback
                        }
                        sosTriggerHandler()
                    }
                )
                
                ActionCard(
                    title = if (isSirenPlaying) "Silence Siren Alarm" else "Sound Siren Alarm",
                    subtitle = if (isSirenPlaying) "Alarm is currently sounding. Tap to silence." else if (!sosSoundEnabled) "SOS sound is OFF in Settings (Tap to start manually)" else "Plays a loud alarm to attract attention",
                    icon = if (isSirenPlaying) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                    color = if (isSirenPlaying) MaterialTheme.colorScheme.error else Color(0xFF00BCD4),
                    onClick = {
                        viewModel.toggleSirenAlarm()
                    }
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))

            // Cancel SOS Button
            var showPinDialog by remember { mutableStateOf(false) }
            
            Button(
                onClick = { 
                    if (countdown != null) {
                        viewModel.cancelEmergencyWithPin("") { success -> 
                            if (success) onNavigateBack()
                        }
                    } else {
                        showPinDialog = true 
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurfaceVariant),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp)
                    .height(56.dp),
                shape = RoundedCornerShape(28.dp)
            ) {
                Icon(Icons.Default.Cancel, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("CANCEL SOS", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
            
            Spacer(modifier = Modifier.height(32.dp))

            if (showPinDialog) {
                CancelSosDialog(
                    onDismiss = { showPinDialog = false },
                    onConfirm = { pin ->
                        viewModel.cancelEmergencyWithPin(pin) { success ->
                            if (success) {
                                showPinDialog = false
                                onNavigateBack()
                            }
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun ActionCard(title: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(color.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = color)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
            }
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun CancelSosDialog(onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var pin by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Cancel Emergency", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text("Enter your 4-digit PIN to cancel the SOS alert and notify your contacts that you are safe.")
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = pin,
                    onValueChange = { if (it.length <= 4) pin = it },
                    label = { Text("PIN") },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.NumberPassword),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(pin) },
                enabled = pin.length == 4,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("Confirm Cancel")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Go Back") }
        }
    )
}
