import re

content = """package com.example.ui.screens

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.ble.BleManager
import com.example.ble.BleManager.BleState
import com.example.ble.BleProtocol

@Composable
fun BleTestScreen(bleManager: BleManager, onNavigateBack: () -> Unit = {}) {
    val context = LocalContext.current
    val connectionState by bleManager.connectionState.collectAsState()
    val deviceName by bleManager.deviceName.collectAsState()
    val deviceMac by bleManager.deviceMac.collectAsState()
    val batteryLevel by bleManager.batteryLevel.collectAsState()
    val sosEvent by bleManager.sosEvent.collectAsState()
    
    val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.ACCESS_FINE_LOCATION)
    } else {
        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
    }
    
    var hasPermissions by remember { mutableStateOf(false) }
    
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        hasPermissions = results.values.all { it }
    }
    
    val enableBtLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (bleManager.isBluetoothEnabled() && hasPermissions && connectionState == BleState.DISCONNECTED) {
            bleManager.scanAndConnect()
        }
    }
    
    LaunchedEffect(Unit) {
        hasPermissions = permissions.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
        if (!hasPermissions) {
            launcher.launch(permissions)
        }
    }
    
    LaunchedEffect(hasPermissions) {
        if (hasPermissions) {
            if (!bleManager.isBluetoothEnabled()) {
                val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                enableBtLauncher.launch(enableBtIntent)
            } else if (connectionState == BleState.DISCONNECTED || connectionState == BleState.ERROR) {
                bleManager.scanAndConnect()
            }
        }
    }
    
    // Show Toast only on first successful connection
    var hasShownToast by remember { mutableStateOf(false) }
    LaunchedEffect(connectionState) {
        if (connectionState == BleState.READY && !hasShownToast) {
            Toast.makeText(context, "ESP32 connected successfully", Toast.LENGTH_LONG).show()
            hasShownToast = true
        }
        if (connectionState == BleState.DISCONNECTED) {
            hasShownToast = false
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            IconButton(onClick = onNavigateBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Text("Raw BLE Connection Test", style = MaterialTheme.typography.titleLarge)
        }
        Spacer(modifier = Modifier.height(32.dp))
        
        val displayState = when(connectionState) {
            BleState.DISCONNECTED -> "Disconnected"
            BleState.ERROR -> "Connection failed"
            BleState.SCANNING -> "Scanning..."
            BleState.CONNECTING -> "Connecting..."
            BleState.CONNECTED -> "Connecting..."
            BleState.DISCOVERING_SERVICES -> "Connecting..."
            BleState.READY -> "Connected"
        }
        
        Text("Device:\nPhysical-SOS-ESP32", style = MaterialTheme.typography.bodyLarge)
        Spacer(modifier = Modifier.height(16.dp))
        
        Text("Connection status:\n$displayState", style = MaterialTheme.typography.bodyLarge)
        Spacer(modifier = Modifier.height(16.dp))
        
        Text("Service:\n${BleProtocol.SERVICE_UUID}", style = MaterialTheme.typography.bodyLarge)
        Spacer(modifier = Modifier.height(16.dp))
        
        Text("Status characteristic:\n${BleProtocol.STATUS_CHARACTERISTIC_UUID}", style = MaterialTheme.typography.bodyLarge)
        Spacer(modifier = Modifier.height(16.dp))
        
        val displayBattery = if (connectionState == BleState.READY) {
            if (batteryLevel != null) "$batteryLevel%" else "Unavailable"
        } else "--"
        
        Text("Battery:\n$displayBattery", style = MaterialTheme.typography.bodyLarge)
        Spacer(modifier = Modifier.height(16.dp))
        
        if (connectionState == BleState.READY) {
            Text("MAC: $deviceMac")
            Spacer(modifier = Modifier.height(16.dp))
            Text("Status Event (Is SOS?):\n$sosEvent", style = MaterialTheme.typography.bodyLarge)
        }
        
        Spacer(modifier = Modifier.weight(1f))
        
        Button(
            onClick = { bleManager.scanAndConnect() },
            enabled = hasPermissions && (connectionState == BleState.DISCONNECTED || connectionState == BleState.ERROR),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Scan Again")
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Button(
            onClick = { bleManager.disconnect() },
            enabled = connectionState != BleState.DISCONNECTED,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Disconnect")
        }
    }
}
"""

with open('app/src/main/java/com/example/ui/screens/BleTestScreen.kt', 'w') as f:
    f.write(content)
