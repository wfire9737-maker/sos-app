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
import androidx.compose.material.icons.automirrored.filled.*
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.BackHandler
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
    val activeEmergency by viewModel.activeEmergency.collectAsState()
    val isSirenPlaying by viewModel.isSirenPlaying.collectAsState()
    val sosSoundEnabled by viewModel.sosSoundEnabled.collectAsState()
    val countdown by viewModel.countdown.collectAsState()
    val contacts by viewModel.contacts.collectAsState()
    val sosWorkflowState by viewModel.sosWorkflowState.collectAsState()
    val context = LocalContext.current
    val primaryContactPhone = contacts.firstOrNull()?.phone?.trim() ?: ""
    val hasValidPhone = primaryContactPhone.isNotEmpty() && primaryContactPhone.matches(Regex("^[+]?[0-9\\s-]{3,15}$"))
    
    val isEmergencyActive = activeEmergency != null || countdown != null

    BackHandler(enabled = isEmergencyActive) {
        // Prevent system and gesture back navigation while emergency is active
    }
    
    val permissionHandler = rememberLocationPermissionHandler {
        // Just trigger the permissions, the background service will pick up the GPS location
    }

    LaunchedEffect(countdown, activeEmergency?.status) {
        if (countdown == null && activeEmergency != null && activeEmergency?.status != "COUNTDOWN") {
            permissionHandler()
        }
    }

    var hasEverBeenActive by remember { mutableStateOf(activeEmergency != null || countdown != null) }
    LaunchedEffect(activeEmergency, countdown) {
        if (activeEmergency != null || countdown != null) {
            hasEverBeenActive = true
        } else if (hasEverBeenActive) {
            onNavigateBack()
        }
    }
    
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
                    IconButton(
                        onClick = {
                            if (!isEmergencyActive) {
                                onNavigateBack()
                            }
                        },
                        enabled = !isEmergencyActive
                    ) {
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
                    val isPending = countdown != null || activeEmergency?.status == "COUNTDOWN"
                    val isPhysicalSos = activeEmergency?.triggerType == "PHYSICAL_BLE_BUTTON"
                    val secondsRemaining = countdown ?: 0

                    if (isPending && isPhysicalSos) {
                        Text(
                            text = if (secondsRemaining > 0) secondsRemaining.toString() else "!",
                            fontSize = 72.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onError
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Emergency SOS Pending",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onError,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (secondsRemaining > 0) "Activating in $secondsRemaining second${if (secondsRemaining == 1) "" else "s"}" else "Activating...",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onError
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Emergency,
                                    contentDescription = "Physical SOS button",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Press the physical SOS button again to cancel",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    } else if (isPending) {
                        Text(
                            text = if (secondsRemaining > 0) secondsRemaining.toString() else "!",
                            fontSize = 72.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onError
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Emergency SOS Pending",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onError,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (secondsRemaining > 0) "Activating in $secondsRemaining second${if (secondsRemaining == 1) "" else "s"}" else "Activating...",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onError
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = {
                                viewModel.cancelEmergencyWithPin("") { success ->
                                    if (success) onNavigateBack()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.surface,
                                contentColor = MaterialTheme.colorScheme.error
                            ),
                            shape = RoundedCornerShape(24.dp),
                            modifier = Modifier
                                .testTag("in_app_cancel_pending_sos_button")
                                .padding(horizontal = 8.dp)
                        ) {
                            Icon(Icons.Default.Cancel, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Cancel SOS", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
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

            if (countdown == null && activeEmergency?.status != "COUNTDOWN") {
                val lat = activeEmergency?.latitude ?: emergencySession.activeAlert?.latitude ?: 0.0
                val lng = activeEmergency?.longitude ?: emergencySession.activeAlert?.longitude ?: 0.0
                val accuracy = activeEmergency?.accuracy ?: 8.0f

                if (lat != 0.0 || lng != 0.0) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.MyLocation,
                                    contentDescription = "Location",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "Location Details",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("Latitude: $lat", fontSize = 14.sp)
                            Text("Longitude: $lng", fontSize = 14.sp)
                            Text("Accuracy: ${accuracy.toInt()} m", fontSize = 14.sp)
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            if (countdown == null && activeEmergency?.status != "COUNTDOWN") {
                // Action Cards
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    ActionCard(
                        title = "Call Emergency Contact",
                        subtitle = if (hasValidPhone) "Instantly dials $primaryContactPhone" else "No valid emergency contact phone configured",
                        icon = Icons.Default.Phone,
                        color = if (hasValidPhone) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline,
                        onClick = {
                            if (hasValidPhone) {
                                if (ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED) {
                                    val intent = Intent(Intent.ACTION_CALL).apply { data = Uri.parse("tel:$primaryContactPhone") }
                                    context.startActivity(intent)
                                } else {
                                    callPermissionLauncher.launch(Manifest.permission.CALL_PHONE)
                                }
                            } else {
                                android.widget.Toast.makeText(context, "Please configure a valid emergency contact phone number in settings.", android.widget.Toast.LENGTH_LONG).show()
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
            }
            
            Spacer(modifier = Modifier.height(32.dp))

            // Cancel SOS Button
            val showCancelPinDialog by viewModel.showCancelPinDialog.collectAsState()
            
            Button(
                onClick = { 
                    if ((countdown != null || activeEmergency?.status == "COUNTDOWN") && viewModel.securityService.getEmergencyPin().isEmpty()) {
                        viewModel.cancelEmergencyWithPin("") { success -> 
                            if (success) onNavigateBack()
                        }
                    } else {
                        viewModel.openCancelPinDialog()
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

            if (showCancelPinDialog) {
                CancelSosDialog(
                    onDismiss = { viewModel.dismissCancelPinDialog() },
                    onConfirm = { pin ->
                        viewModel.cancelEmergencyWithPin(pin) { success ->
                            if (success) {
                                viewModel.dismissCancelPinDialog()
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
