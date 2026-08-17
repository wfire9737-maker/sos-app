package com.example.ui.screens
import com.example.model.PermissionsState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.ui.GuardianViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver

fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PermissionsScreen(viewModel: GuardianViewModel, onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    val activity = context.findActivity()
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME || event == Lifecycle.Event.ON_START) {
                viewModel.refreshPermissions(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.refreshPermissions(context)
    }

    val permissionsState by viewModel.permissionsState.collectAsState()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Permission Manager", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Smart SOS needs several permissions to reliably detect emergencies and request help on your behalf.", style = MaterialTheme.typography.bodyLarge)
            
            // Location
            PermissionSection(
                context = context,
                activity = activity,
                title = "Location",
                description = "Required to share your exact location with emergency contacts.",
                icon = Icons.Filled.LocationOn,
                permissions = listOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                isGranted = permissionsState.locationGranted,
                onUpdate = { viewModel.refreshPermissions(context) }
            )

            // Background Location
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                if (permissionsState.locationGranted) {
                    PermissionSection(
                        context = context,
                        activity = activity,
                        title = "Background Location",
                        description = "Required for active emergency tracking while the app is in the background.",
                        icon = Icons.Filled.ShareLocation,
                        permissions = listOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION),
                        isGranted = permissionsState.backgroundLocationGranted,
                        onUpdate = { viewModel.refreshPermissions(context) }
                    )
                }
            }

            // Calls
            PermissionSection(
                context = context,
                activity = activity,
                title = "Phone Calls",
                description = "Required to automatically call emergency contacts or 911 when SOS is triggered.",
                icon = Icons.Filled.Call,
                permissions = listOf(Manifest.permission.CALL_PHONE),
                isGranted = permissionsState.callsGranted,
                onUpdate = { viewModel.refreshPermissions(context) }
            )

            // SMS
            PermissionSection(
                context = context,
                activity = activity,
                title = "SMS Messages",
                description = "Required to send automatic SOS text messages with your live location.",
                icon = Icons.AutoMirrored.Filled.Message,
                permissions = listOf(Manifest.permission.SEND_SMS),
                isGranted = permissionsState.smsGranted,
                onUpdate = { viewModel.refreshPermissions(context) }
            )

            // Contacts
            PermissionSection(
                context = context,
                activity = activity,
                title = "Contacts",
                description = "Required to select your emergency contacts from your phonebook.",
                icon = Icons.Filled.Contacts,
                permissions = listOf(Manifest.permission.READ_CONTACTS),
                isGranted = permissionsState.contactsGranted,
                onUpdate = { viewModel.refreshPermissions(context) }
            )

            // Notifications
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                PermissionSection(
                    context = context,
                    activity = activity,
                    title = "Notifications",
                    description = "Required to alert you of active safety timers and ongoing emergencies.",
                    icon = Icons.Filled.Notifications,
                    permissions = listOf(Manifest.permission.POST_NOTIFICATIONS),
                    isGranted = permissionsState.notificationsGranted,
                    onUpdate = { viewModel.refreshPermissions(context) }
                )
            }
            
            // Audio (Voice SOS)
            PermissionSection(
                context = context,
                activity = activity,
                title = "Microphone",
                description = "Required for the Voice SOS feature to listen for your wake word.",
                icon = Icons.Filled.Mic,
                permissions = listOf(Manifest.permission.RECORD_AUDIO),
                isGranted = permissionsState.audioGranted,
                onUpdate = { viewModel.refreshPermissions(context) }
            )

            // System Alert Window (Overlay)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(32.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Background Calls & Alerts", fontWeight = FontWeight.Bold)
                        Text("Required to make automatic SOS calls and show alerts even when the app is closed.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    if (permissionsState.overlayGranted) {
                        Text("Granted", color = androidx.compose.ui.graphics.Color(0xFF4CAF50), fontWeight = FontWeight.Bold)
                    } else {
                        Button(onClick = {
                            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}"))
                            context.startActivity(intent)
                        }) {
                            Text("Grant")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PermissionSection(
    context: Context,
    activity: Activity?,
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    permissions: List<String>,
    isGranted: Boolean,
    onUpdate: () -> Unit
) {
    var showRationaleDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        onUpdate()
        val allGranted = result.values.all { it }
        if (!allGranted && activity != null) {
            val shouldShowRationale = permissions.any { ActivityCompat.shouldShowRequestPermissionRationale(activity, it) }
            if (!shouldShowRationale) {
                showSettingsDialog = true
            }
        }
    }

    if (showRationaleDialog) {
        AlertDialog(
            onDismissRequest = { showRationaleDialog = false },
            title = { Text("Permission Required") },
            text = { Text(description) },
            confirmButton = {
                Button(onClick = {
                    showRationaleDialog = false
                    launcher.launch(permissions.toTypedArray())
                }) {
                    Text("Proceed")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRationaleDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showSettingsDialog) {
        AlertDialog(
            onDismissRequest = { showSettingsDialog = false },
            title = { Text("Permission Denied") },
            text = { Text("This permission is permanently denied. You must enable it in system settings to use this feature.") },
            confirmButton = {
                Button(onClick = {
                    showSettingsDialog = false
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.parse("package:${context.packageName}")
                    }
                    context.startActivity(intent)
                }) {
                    Text("Open Settings")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSettingsDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(32.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold)
                Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(modifier = Modifier.width(16.dp))
            if (isGranted) {
                Text("Granted", color = androidx.compose.ui.graphics.Color(0xFF4CAF50), fontWeight = FontWeight.Bold)
            } else {
                Button(onClick = {
                    if (activity != null) {
                        val shouldShowRationale = permissions.any { ActivityCompat.shouldShowRequestPermissionRationale(activity, it) }
                        if (shouldShowRationale) {
                            showRationaleDialog = true
                        } else {
                            launcher.launch(permissions.toTypedArray())
                        }
                    } else {
                        launcher.launch(permissions.toTypedArray())
                    }
                }) {
                    Text("Grant")
                }
            }
        }
    }
}
