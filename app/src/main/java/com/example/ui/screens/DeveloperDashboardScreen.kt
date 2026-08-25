package com.example.ui.screens

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.content.Context

import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.content.IntentFilter
import kotlinx.coroutines.delay
import android.content.ClipData
import android.content.ClipboardManager
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import android.content.pm.PackageManager
import android.location.LocationManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowBack

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.*
import androidx.compose.material3.Checkbox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.service.AuthState
import com.example.ui.GuardianViewModel

import com.example.ui.rememberLocationPermissionHandler

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.google.firebase.FirebaseApp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeveloperDashboardScreen(
    viewModel: GuardianViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current

    var showResetDialog by remember { mutableStateOf(false) }
    var resetAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    var resetTitle by remember { mutableStateOf("") }
    var resetMessage by remember { mutableStateOf("") }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text(resetTitle) },
            text = { Text(resetMessage) },
            confirmButton = {
                TextButton(onClick = { 
                    resetAction?.invoke() 
                    showResetDialog = false 
                }) {
                    Text("Confirm", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    
    // States
    val isNetworkAvailable by viewModel.isNetworkAvailable.collectAsState(initial = false)
    val authState by viewModel.authState.collectAsState()
    val devices by viewModel.devices.collectAsState(initial = emptyList())
    val alerts by viewModel.alerts.collectAsState(initial = emptyList())
    
    // Calculated Statuses
    val bluetoothAdapter = context.getSystemService(android.bluetooth.BluetoothManager::class.java)?.adapter
    val isBluetoothEnabled = bluetoothAdapter?.isEnabled == true
    
    val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    val isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
    
    val isFirebaseInitialized = FirebaseApp.getApps(context).isNotEmpty()
    
    val isLocationPermissionGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    val isCameraPermissionGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
    val isMicPermissionGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
    val allPermissionsGranted = isLocationPermissionGranted && isCameraPermissionGranted && isMicPermissionGranted
    
    val anyDeviceConnected = devices.any { it.status == "CONNECTED" }
    val firstDevice = devices.firstOrNull()
    val batteryLevel = firstDevice?.batteryLevel ?: -1
    
    val lastAlert = alerts.maxByOrNull { it.timestamp }
    
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Developer Dashboard", fontWeight = FontWeight.Bold) },
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
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            StatusItem("Bluetooth Status", isBluetoothEnabled, if (isBluetoothEnabled) "Connected" else "Waiting")
            StatusItem("GPS Status", isGpsEnabled, if (isGpsEnabled) "Connected" else "Waiting")
            StatusItem("Internet Status", isNetworkAvailable, if (isNetworkAvailable) "Connected" else "Error")
            
            val firebaseStatusStr = if (isFirebaseInitialized && authState is AuthState.Success) "Connected" else if (isFirebaseInitialized) "Waiting" else "Error"
            val firebaseColor = if (isFirebaseInitialized && authState is AuthState.Success) Color.Green else if (isFirebaseInitialized) Color.Yellow else Color.Red
            StatusItemCustomColor("Firebase Status", firebaseStatusStr, firebaseColor)
            
            StatusItem("Permissions Status", allPermissionsGranted, if (allPermissionsGranted) "Connected" else "Waiting")
            StatusItem("Device Connection", anyDeviceConnected, if (anyDeviceConnected) "Connected" else "Waiting")
            
            val batteryStatusStr = if (batteryLevel > 20) "$batteryLevel% (Connected)" else if (batteryLevel > 0) "$batteryLevel% (Waiting)" else "Error"
            val batteryColor = if (batteryLevel > 20) Color.Green else if (batteryLevel > 0) Color.Yellow else Color.Red
            StatusItemCustomColor("Battery Level", batteryStatusStr, batteryColor)
            
            val timeString = if (lastAlert != null) {
                SimpleDateFormat("MM/dd/yyyy HH:mm:ss", Locale.getDefault()).format(Date(lastAlert.timestamp))
            } else {
                "No SOS Recorded"
            }
            StatusItemCustomColor("Last SOS Time", timeString, Color.Green)



            Spacer(modifier = Modifier.height(16.dp))
            Text("BLE Diagnostics (Real)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

            val isEsp32Connected by viewModel.isEsp32Connected.collectAsState()
            val activeEmergency by viewModel.activeEmergency.collectAsState()
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatusItemCustomColor("BLE Status", if (isEsp32Connected) "Connected" else "Disconnected", if (isEsp32Connected) Color.Green else Color.Red)
                    StatusItemCustomColor("SOS State", if (activeEmergency != null) "ACTIVE" else "INACTIVE", if (activeEmergency != null) Color.Red else Color.Green)
                }
            }
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { viewModel.startEsp32Polling() }, modifier = Modifier.weight(1f)) {
                    Text("Start Scan", maxLines = 1, style = MaterialTheme.typography.labelSmall)
                }
                Button(onClick = { viewModel.stopEsp32Polling() }, modifier = Modifier.weight(1f)) {
                    Text("Disconnect", maxLines = 1, style = MaterialTheme.typography.labelSmall)
                }
                Button(onClick = { viewModel.resetEsp32() }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) {
                    Text("Reset ESP32", maxLines = 1, style = MaterialTheme.typography.labelSmall)
                }
            }


            Spacer(modifier = Modifier.height(24.dp))
            Text("Module 7 - Permission Testing", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

            val permissionsState by viewModel.permissionsState.collectAsState()
            val isBluetoothPermissionGranted = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
            } else {
                ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH) == PackageManager.PERMISSION_GRANTED
            }

            StatusItemCustomColor("Location Permission", if(permissionsState.locationGranted) "Granted" else "Denied", if(permissionsState.locationGranted) Color.Green else Color.Red)
            StatusItemCustomColor("Bluetooth Permission", if(isBluetoothPermissionGranted) "Granted" else "Denied", if(isBluetoothPermissionGranted) Color.Green else Color.Red)
            StatusItemCustomColor("SMS Permission", if(permissionsState.smsGranted) "Granted" else "Denied", if(permissionsState.smsGranted) Color.Green else Color.Red)
            StatusItemCustomColor("Call Permission", if(permissionsState.callsGranted) "Granted" else "Denied", if(permissionsState.callsGranted) Color.Green else Color.Red)
            StatusItemCustomColor("Notification Permission", if(permissionsState.notificationsGranted) "Granted" else "Denied", if(permissionsState.notificationsGranted) Color.Green else Color.Red)

            val multiplePermissionsLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestMultiplePermissions()
            ) {
                viewModel.refreshPermissions(context)
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { 
                    viewModel.refreshPermissions(context)
                }, modifier = Modifier.weight(1f)) {
                    Text("Check Permissions")
                }
                
                Button(onClick = { 
                    val permsToRequest = mutableListOf<String>()
                    if (!permissionsState.locationGranted) permsToRequest.add(Manifest.permission.ACCESS_FINE_LOCATION)
                    if (!isBluetoothPermissionGranted) {
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                            permsToRequest.add(Manifest.permission.BLUETOOTH_CONNECT)
                            permsToRequest.add(Manifest.permission.BLUETOOTH_SCAN)
                        } else {
                            permsToRequest.add(Manifest.permission.BLUETOOTH)
                        }
                    }
                    if (!permissionsState.smsGranted) permsToRequest.add(Manifest.permission.SEND_SMS)
                    if (!permissionsState.callsGranted) permsToRequest.add(Manifest.permission.CALL_PHONE)
                    if (!permissionsState.notificationsGranted && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                        permsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
                    }
                    if (permsToRequest.isNotEmpty()) {
                        multiplePermissionsLauncher.launch(permsToRequest.toTypedArray())
                    }
                }, modifier = Modifier.weight(1f)) {
                    Text("Request Missing Permissions", maxLines = 1, style = MaterialTheme.typography.labelSmall)
                }
            }

            Button(onClick = { 
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.parse("package:${context.packageName}")
                }
                context.startActivity(intent)
            }, modifier = Modifier.fillMaxWidth()) {
                Text("Open App Settings")
            }

            Spacer(modifier = Modifier.height(24.dp))
            Text("Module 8 - Developer Logs", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

            val devLogs by viewModel.developerLogs.collectAsState()

            // Buttons
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { viewModel.clearDeveloperLogs() }, modifier = Modifier.weight(1f)) {
                    Text("Clear")
                }
                Button(onClick = { 
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val text = devLogs.joinToString("\n") { "${it.timestamp}: ${it.event} [${it.status}]" }
                    clipboard.setPrimaryClip(ClipData.newPlainText("Developer Logs", text))
                }, modifier = Modifier.weight(1f)) {
                    Text("Copy")
                }
                Button(onClick = {
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        val text = devLogs.joinToString("\n") { "${it.timestamp}: ${it.event} [${it.status}]" }
                        putExtra(Intent.EXTRA_TEXT, text)
                        putExtra(Intent.EXTRA_SUBJECT, "Guardian App Logs")
                    }
                    context.startActivity(Intent.createChooser(intent, "Export Logs"))
                }, modifier = Modifier.weight(1f)) {
                    Text("Export")
                }
            }

            // Manual event triggers to generate logs for testing
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { viewModel.addDeveloperLog("Bluetooth Connected", "SUCCESS") }, modifier = Modifier.weight(1f)) {
                    Text("BT Event", style = MaterialTheme.typography.labelSmall)
                }
                Button(onClick = { viewModel.addDeveloperLog("SOS Triggered", "WARNING") }, modifier = Modifier.weight(1f)) {
                    Text("SOS Event", style = MaterialTheme.typography.labelSmall)
                }
                Button(onClick = { viewModel.addDeveloperLog("Network Timeout", "ERROR") }, modifier = Modifier.weight(1f)) {
                    Text("Err Event", style = MaterialTheme.typography.labelSmall)
                }
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(devLogs) { log ->
                        val timeStr = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(log.timestamp))
                        val color = when(log.status) {
                            "ERROR", "CRITICAL" -> Color.Red
                            "WARNING" -> Color.Yellow
                            "SUCCESS" -> Color.Green
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Text(timeStr, style = MaterialTheme.typography.labelSmall, modifier = Modifier.width(70.dp))
                            Text(log.event, style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(1f))
                            Text(log.status, style = MaterialTheme.typography.labelSmall, color = color)
                        }
                    }
                }
            }

        
            Spacer(modifier = Modifier.height(24.dp))
            Text("Module 9 - Performance Monitor", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

            var memoryUsage by remember { mutableStateOf(0L) }
            var networkType by remember { mutableStateOf("Unknown") }
            var currentBattery by remember { mutableStateOf(-1) }
            var gpsAccuracy by remember { mutableStateOf("Unknown") }

            val appVersion = try {
                context.packageManager.getPackageInfo(context.packageName, 0).versionName
            } catch (e: Exception) {
                "Unknown"
            }
            val androidVersion = android.os.Build.VERSION.RELEASE
            val deviceModel = android.os.Build.MODEL
            
            LaunchedEffect(Unit) {
                while(true) {
                    val runtime = Runtime.getRuntime()
                    memoryUsage = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024)
                    
                    val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
                    val network = connectivityManager.activeNetwork
                    val capabilities = connectivityManager.getNetworkCapabilities(network)
                    networkType = when {
                        capabilities == null -> "None"
                        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "WiFi"
                        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Cellular"
                        else -> "Other"
                    }
                    
                    val batteryStatus: Intent? = IntentFilter(Intent.ACTION_BATTERY_CHANGED).let { ifilter ->
                        context.registerReceiver(null, ifilter)
                    }
                    currentBattery = batteryStatus?.let { intent ->
                        val level: Int = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                        val scale: Int = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                        if (scale > 0) (level * 100 / scale) else -1
                    } ?: -1
                    
                    gpsAccuracy = if (isLocationPermissionGranted && isGpsEnabled) "High" else "Low"
                    
                    delay(2000)
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatusItemCustomColor("App Version", appVersion ?: "Unknown", MaterialTheme.colorScheme.onSurfaceVariant)
                    StatusItemCustomColor("Android Version", androidVersion, MaterialTheme.colorScheme.onSurfaceVariant)
                    StatusItemCustomColor("Device Model", deviceModel, MaterialTheme.colorScheme.onSurfaceVariant)
                    StatusItemCustomColor("Memory Usage", "${memoryUsage} MB", if (memoryUsage > 200) Color.Yellow else Color.Green)
                    StatusItemCustomColor("Network Type", networkType, if (networkType == "None") Color.Red else Color.Green)
                    StatusItemCustomColor("GPS Accuracy", gpsAccuracy, if (gpsAccuracy == "High") Color.Green else Color.Yellow)
                    StatusItemCustomColor("Battery Level", "${currentBattery}%", if (currentBattery > 20) Color.Green else Color.Red)
                    StatusItemCustomColor("Bluetooth State", if (isBluetoothEnabled) "ON" else "OFF", if (isBluetoothEnabled) Color.Green else Color.Red)
                }
            }

        
            Spacer(modifier = Modifier.height(24.dp))
            Text("Module 10 - Reset & Exit", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { 
                        resetTitle = "Clear Test Data"
                        resetMessage = "Are you sure you want to delete all test records?"
                        resetAction = {
                            viewModel.deleteTestRecords()
                            viewModel.clearDeveloperLogs()
                            viewModel.cleanDiagnosticsLog()
                            viewModel.clearCommLogs()
                        }
                        showResetDialog = true
                    }, 
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Clear Test Data", maxLines = 1, style = MaterialTheme.typography.labelSmall)
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { 
                        resetTitle = "Disable Developer Mode"
                        resetMessage = "Are you sure you want to disable Developer Mode and return?"
                        resetAction = {
                            onNavigateBack()
                        }
                        showResetDialog = true
                    }, 
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Disable Developer Mode", maxLines = 1, style = MaterialTheme.typography.labelSmall)
                }
                
                Button(
                    onClick = { 
                        resetTitle = "Restart Test Environment"
                        resetMessage = "Are you sure you want to restart the test environment?"
                        resetAction = {
                            viewModel.deleteTestRecords()
                            viewModel.clearDeveloperLogs()
                            viewModel.cleanDiagnosticsLog()
                            viewModel.clearCommLogs()
                        }
                        showResetDialog = true
                    }, 
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Restart Test Environment", maxLines = 1, style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}



@Composable
fun StatusItem(title: String, isOk: Boolean, statusText: String) {
    val color = if (isOk) Color.Green else if (statusText == "Waiting") Color.Yellow else Color.Red
    StatusItemCustomColor(title, statusText, color)
}




@Composable
fun StatusItemCustomColor(title: String, statusText: String, color: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .background(color, CircleShape)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(statusText, style = MaterialTheme.typography.bodyMedium, color = color)
        }
    }
}
