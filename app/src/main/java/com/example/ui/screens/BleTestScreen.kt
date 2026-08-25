package com.example.ui.screens

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.ble.BleManager
import com.example.ble.BleManager.BleState
import com.example.ble.BleProtocol
import com.example.ble.HardwareGpsLocation
import com.example.ble.HardwareGpsState
import com.example.ble.Mpu6050Reading
import com.example.ble.MpuHardwareState
import com.example.ble.MotionState
import com.example.ui.theme.SafetyGreen
import com.example.ui.theme.AlertOrange
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BleTestScreen(
    bleManager: BleManager,
    onNavigateBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val connectionState by bleManager.connectionState.collectAsState()
    val deviceName by bleManager.deviceName.collectAsState()
    val deviceMac by bleManager.deviceMac.collectAsState()
    val statusString by bleManager.statusString.collectAsState()
    val batteryDisplay by bleManager.batteryDisplay.collectAsState()
    val rssi by bleManager.rssi.collectAsState()
    val sosEvent by bleManager.sosEvent.collectAsState()
    val lastTimestamp by bleManager.lastStatusTimestamp.collectAsState()
    val lastError by bleManager.lastErrorMessage.collectAsState()
    val serviceFound by bleManager.serviceFound.collectAsState()
    val notificationSubscribed by bleManager.notificationSubscribed.collectAsState()
    val statusNotificationSubscribed by bleManager.statusNotificationSubscribed.collectAsState()
    val gpsNotificationSubscribed by bleManager.gpsNotificationSubscribed.collectAsState()
    val gpsCharacteristicFound by bleManager.gpsCharacteristicFound.collectAsState()
    val hardwareGpsState by bleManager.hardwareGpsState.collectAsState()
    val latestHardwareGpsLocation by bleManager.latestHardwareGpsLocation.collectAsState()
    val gpsRawString by bleManager.gpsRawString.collectAsState()
    val lastGpsTimestamp by bleManager.lastGpsTimestamp.collectAsState()
    val mpuNotificationSubscribed by bleManager.mpuNotificationSubscribed.collectAsState()
    val mpuCharacteristicFound by bleManager.mpuCharacteristicFound.collectAsState()
    val mpuHardwareState by bleManager.mpuHardwareState.collectAsState()
    val latestMpuReading by bleManager.latestMpuReading.collectAsState()
    val motionState by bleManager.motionState.collectAsState()
    val mpuRawString by bleManager.mpuRawString.collectAsState()
    val lastMpuTimestamp by bleManager.lastMpuTimestamp.collectAsState()
    val mpuRecentReadings by bleManager.mpuRecentReadings.collectAsState()
    val sosEventCount by bleManager.sosEventCount.collectAsState()
    val lastSosEvent by bleManager.lastSosEvent.collectAsState()
    val sosEventList by bleManager.sosEventList.collectAsState()

    val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        arrayOf(
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    } else {
        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    var hasPermissions by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
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
            permissionLauncher.launch(permissions)
        }
    }

    LaunchedEffect(hasPermissions) {
        if (hasPermissions) {
            if (!bleManager.isBluetoothEnabled()) {
                val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                enableBtLauncher.launch(enableBtIntent)
            } else if (connectionState == BleState.DISCONNECTED ||
                connectionState == BleState.DEVICE_NOT_FOUND ||
                connectionState == BleState.CONNECTION_FAILED ||
                connectionState == BleState.ERROR
            ) {
                bleManager.scanAndConnect()
            }
        }
    }

    // Toast requirement: Show once ONLY after connection, service discovery, and characteristic verification succeed
    var hasShownToast by remember { mutableStateOf(false) }
    LaunchedEffect(connectionState) {
        if (connectionState == BleState.CONNECTED && !hasShownToast) {
            Toast.makeText(context, "ESP32 connected successfully", Toast.LENGTH_LONG).show()
            hasShownToast = true
        } else if (connectionState == BleState.DISCONNECTED ||
            connectionState == BleState.DEVICE_NOT_FOUND ||
            connectionState == BleState.CONNECTION_FAILED
        ) {
            hasShownToast = false
        }
    }

    val displayStatus = when (connectionState) {
        BleState.SCANNING -> "Scanning..."
        BleState.CONNECTING,
        BleState.DISCOVERING_SERVICES,
        BleState.SUBSCRIBING_STATUS_NOTIFICATIONS,
        BleState.SUBSCRIBING_GPS_NOTIFICATIONS,
        BleState.SUBSCRIBING_MPU_NOTIFICATIONS,
        BleState.READING_BATTERY,
        BleState.READING_GPS,
        BleState.READING_MPU -> "Connecting..."
        BleState.CONNECTED,
        BleState.READY -> "Connected"
        BleState.DEVICE_NOT_FOUND -> "ESP32 not found"
        BleState.CONNECTION_FAILED,
        BleState.ERROR -> "Connection failed"
        BleState.DISCONNECTED -> "Disconnected"
    }

    val statusColor = when (connectionState) {
        BleState.CONNECTED,
        BleState.READY -> SafetyGreen
        BleState.SCANNING -> MaterialTheme.colorScheme.primary
        BleState.CONNECTING,
        BleState.DISCOVERING_SERVICES,
        BleState.SUBSCRIBING_STATUS_NOTIFICATIONS,
        BleState.SUBSCRIBING_GPS_NOTIFICATIONS,
        BleState.SUBSCRIBING_MPU_NOTIFICATIONS,
        BleState.READING_BATTERY,
        BleState.READING_GPS,
        BleState.READING_MPU -> AlertOrange
        BleState.DEVICE_NOT_FOUND,
        BleState.CONNECTION_FAILED,
        BleState.ERROR -> MaterialTheme.colorScheme.error
        BleState.DISCONNECTED -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    val isConnected = connectionState == BleState.CONNECTED || connectionState == BleState.READY

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "ESP32 BLE Hardware",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Physical Button & GATT Interface",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("ble_back_button")
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to Dashboard"
                        )
                    }
                },
                actions = {
                    if (isConnected) {
                        Surface(
                            shape = CircleShape,
                            color = SafetyGreen.copy(alpha = 0.2f),
                            modifier = Modifier.padding(end = 12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(SafetyGreen, CircleShape)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    "LIVE",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SafetyGreen
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(2.dp))
            }

            // Connection Status Banner
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .background(statusColor.copy(alpha = 0.15f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.Bluetooth,
                                        contentDescription = "Bluetooth",
                                        tint = statusColor,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Column {
                                    Text(
                                        "Device",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        BleProtocol.DEVICE_NAME,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            // Connection status pill badge
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = statusColor.copy(alpha = 0.18f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .background(statusColor, CircleShape)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        displayStatus,
                                        color = statusColor,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }

                        if (lastError != null && !isConnected) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        lastError ?: "",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Physical SOS Alert Banner (if triggered via BLE notification)
            if (sosEvent || statusString?.contains("SOS_TRIGGERED", ignoreCase = true) == true) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Emergency,
                                contentDescription = "SOS Triggered",
                                tint = MaterialTheme.colorScheme.onError,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    "🚨 PHYSICAL SOS BUTTON PRESSED!",
                                    color = MaterialTheme.colorScheme.onError,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 15.sp
                                )
                                Text(
                                    if (lastSosEvent != null) {
                                        "Event #${lastSosEvent?.eventId} received • Total events: $sosEventCount"
                                    } else {
                                        "ESP32 GPIO 4 hardware button event received via BLE notification."
                                    },
                                    color = MaterialTheme.colorScheme.onError.copy(alpha = 0.9f),
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }

            // Hardware Button Events Summary Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Hardware Button Telemetry",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (sosEventCount > 0) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Text(
                                    text = "$sosEventCount Presses Received",
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = if (sosEventCount > 0) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        TelemetryRow(
                            label = "Latest Event ID",
                            value = lastSosEvent?.eventId ?: "None"
                        )
                        TelemetryRow(
                            label = "Latest Raw Payload",
                            value = lastSosEvent?.rawPayload ?: (statusString ?: "None")
                        )
                        TelemetryRow(
                            label = "Continuous Listening",
                            value = if (notificationSubscribed) "Active (No Reconnect Needed)" else "Pending Subscription"
                        )
                    }
                }
            }

            // Characteristics and Values Section
            item {
                Text(
                    "GATT Characteristics & Payload",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Service UUID
                        GattItemRow(
                            label = "Service UUID",
                            value = BleProtocol.SERVICE_UUID.toString(),
                            verified = serviceFound
                        )

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                        // Status Characteristic & Value
                        Column {
                            GattItemRow(
                                label = "Status Characteristic UUID",
                                value = BleProtocol.STATUS_CHARACTERISTIC_UUID.toString(),
                                verified = notificationSubscribed || isConnected
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "Current Status Payload:",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (sosEvent) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.secondaryContainer
                                ) {
                                    Text(
                                        text = statusString ?: "Not received yet",
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 13.sp,
                                        color = if (sosEvent) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                        // Battery Characteristic & Value
                        Column {
                            GattItemRow(
                                label = "Battery Characteristic UUID",
                                value = BleProtocol.BATTERY_CHARACTERISTIC_UUID.toString(),
                                verified = isConnected
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "Battery Level:",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant
                                ) {
                                    Text(
                                        text = if (isConnected) batteryDisplay else "--",
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                        // GPS Characteristic & Value
                        Column {
                            GattItemRow(
                                label = "NEO-6M GPS Characteristic UUID",
                                value = BleProtocol.GPS_CHARACTERISTIC_UUID.toString(),
                                verified = gpsCharacteristicFound || gpsNotificationSubscribed
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "Raw GPS Payload:",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = when (hardwareGpsState) {
                                        is HardwareGpsState.ValidLocation -> MaterialTheme.colorScheme.primaryContainer
                                        is HardwareGpsState.WaitingForFix -> MaterialTheme.colorScheme.tertiaryContainer
                                        else -> MaterialTheme.colorScheme.surfaceVariant
                                    }
                                ) {
                                    Text(
                                        text = gpsRawString ?: "No GPS data yet",
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 12.sp,
                                        color = when (hardwareGpsState) {
                                            is HardwareGpsState.ValidLocation -> MaterialTheme.colorScheme.onPrimaryContainer
                                            is HardwareGpsState.WaitingForFix -> MaterialTheme.colorScheme.onTertiaryContainer
                                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                                        }
                                    )
                                }
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                        // MPU6050 Characteristic & Value
                        Column {
                            GattItemRow(
                                label = "MPU6050 IMU Characteristic UUID",
                                value = BleProtocol.MPU6050_CHARACTERISTIC_UUID.toString(),
                                verified = mpuCharacteristicFound || mpuNotificationSubscribed
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "Raw MPU6050 Payload:",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = when (mpuHardwareState) {
                                        is MpuHardwareState.Receiving -> MaterialTheme.colorScheme.primaryContainer
                                        is MpuHardwareState.Connecting -> MaterialTheme.colorScheme.tertiaryContainer
                                        else -> MaterialTheme.colorScheme.surfaceVariant
                                    }
                                ) {
                                    Text(
                                        text = mpuRawString ?: "No MPU data yet",
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp,
                                        color = when (mpuHardwareState) {
                                            is MpuHardwareState.Receiving -> MaterialTheme.colorScheme.onPrimaryContainer
                                            is MpuHardwareState.Connecting -> MaterialTheme.colorScheme.onTertiaryContainer
                                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Real Hardware NEO-6M GPS Telemetry Card
            item {
                Text(
                    "NEO-6M GPS Telemetry (ESP32 Hardware)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Fix Status",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = when (hardwareGpsState) {
                                    is HardwareGpsState.ValidLocation -> SafetyGreen.copy(alpha = 0.2f)
                                    is HardwareGpsState.WaitingForFix -> AlertOrange.copy(alpha = 0.2f)
                                    else -> MaterialTheme.colorScheme.surfaceVariant
                                }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = when (hardwareGpsState) {
                                            is HardwareGpsState.ValidLocation -> Icons.Default.LocationOn
                                            is HardwareGpsState.WaitingForFix -> Icons.Default.SatelliteAlt
                                            else -> Icons.Default.LocationOff
                                        },
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp),
                                        tint = when (hardwareGpsState) {
                                            is HardwareGpsState.ValidLocation -> SafetyGreen
                                            is HardwareGpsState.WaitingForFix -> AlertOrange
                                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                                        }
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = when (hardwareGpsState) {
                                            is HardwareGpsState.ValidLocation -> "Valid GPS Fix"
                                            is HardwareGpsState.WaitingForFix -> "Waiting for Fix (NO_FIX)"
                                            is HardwareGpsState.Error -> (hardwareGpsState as HardwareGpsState.Error).message
                                            is HardwareGpsState.Unavailable -> "Module Inactive / Disconnected"
                                        },
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = when (hardwareGpsState) {
                                            is HardwareGpsState.ValidLocation -> SafetyGreen
                                            is HardwareGpsState.WaitingForFix -> AlertOrange
                                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                                        }
                                    )
                                }
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                        TelemetryRow(
                            label = "Location Source",
                            value = "ESP32_NEO6M"
                        )

                        TelemetryRow(
                            label = "Latitude",
                            value = latestHardwareGpsLocation?.let { String.format(Locale.US, "%.6f", it.latitude) } ?: "Waiting for fix"
                        )

                        TelemetryRow(
                            label = "Longitude",
                            value = latestHardwareGpsLocation?.let { String.format(Locale.US, "%.6f", it.longitude) } ?: "Waiting for fix"
                        )

                        TelemetryRow(
                            label = "GPS Notifications",
                            value = if (gpsNotificationSubscribed) "Active (Subscribed)" else "Pending"
                        )

                        TelemetryRow(
                            label = "Freshness",
                            value = latestHardwareGpsLocation?.getFreshnessDescription() ?: (if (lastGpsTimestamp > 0) SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(lastGpsTimestamp)) else "None")
                        )
                    }
                }
            }

            // Real Hardware MPU6050 IMU Telemetry Card
            item {
                Text(
                    "MPU6050 Motion & Fall Telemetry (ESP32 Hardware)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Motion State Header
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Motion State",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            val motionBadgeColor = when (motionState) {
                                MotionState.POSSIBLE_FALL -> MaterialTheme.colorScheme.error
                                MotionState.POSSIBLE_IMPACT -> AlertOrange
                                MotionState.POSSIBLE_FREE_FALL -> MaterialTheme.colorScheme.tertiary
                                MotionState.NORMAL -> SafetyGreen
                            }
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = motionBadgeColor.copy(alpha = 0.18f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .background(motionBadgeColor, CircleShape)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = motionState.displayName,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = motionBadgeColor
                                    )
                                }
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                        // Acceleration Metrics
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Acceleration (3-Axis)",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = latestMpuReading?.let {
                                    "X: ${String.format(Locale.US, "%.2f", it.accelerationX)}g  Y: ${String.format(Locale.US, "%.2f", it.accelerationY)}g  Z: ${String.format(Locale.US, "%.2f", it.accelerationZ)}g"
                                } ?: "Waiting for MPU stream",
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        // Total Magnitude
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Total Magnitude",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            val mag = latestMpuReading?.accelerationMagnitudeG
                            val magColor = when {
                                mag == null -> MaterialTheme.colorScheme.onSurface
                                mag > 2.7 -> MaterialTheme.colorScheme.error
                                mag < 0.5 -> AlertOrange
                                else -> SafetyGreen
                            }
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = magColor.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = if (mag != null) String.format(Locale.US, "%.2f g", mag) else "--",
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = magColor
                                )
                            }
                        }

                        // Gyroscope Metrics
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Gyroscope (3-Axis)",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = latestMpuReading?.let {
                                    "X: ${String.format(Locale.US, "%.1f", it.gyroX)}°/s  Y: ${String.format(Locale.US, "%.1f", it.gyroY)}°/s  Z: ${String.format(Locale.US, "%.1f", it.gyroZ)}°/s"
                                } ?: "Waiting for MPU stream",
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        TelemetryRow(
                            label = "MPU Notifications",
                            value = if (mpuNotificationSubscribed) "Active (Subscribed)" else "Pending"
                        )

                        TelemetryRow(
                            label = "Freshness",
                            value = latestMpuReading?.getFreshnessDescription() ?: (if (lastMpuTimestamp > 0) SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(lastMpuTimestamp)) else "None")
                        )
                    }
                }
            }

            // Diagnostics & Telemetry Card
            item {
                Text(
                    "Connection Telemetry",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        TelemetryRow(label = "MAC Address", value = deviceMac ?: "00:00:00:00:00:00")
                        TelemetryRow(
                            label = "Signal Strength (RSSI)",
                            value = if (rssi != null && isConnected) "$rssi dBm" else "N/A"
                        )
                        TelemetryRow(
                            label = "Status Notifications",
                            value = if (statusNotificationSubscribed) "Active (Subscribed)" else "Pending / Inactive"
                        )
                        TelemetryRow(
                            label = "GPS Notifications",
                            value = if (gpsNotificationSubscribed) "Active (Subscribed)" else "Pending / Inactive"
                        )
                        TelemetryRow(
                            label = "MPU6050 Notifications",
                            value = if (mpuNotificationSubscribed) "Active (Subscribed)" else "Pending / Inactive"
                        )
                        TelemetryRow(
                            label = "Last Received",
                            value = if (lastTimestamp > 0) {
                                SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(lastTimestamp))
                            } else "None"
                        )
                    }
                }
            }

            // Event History List
            if (sosEventList.isNotEmpty()) {
                item {
                    Text(
                        "Hardware Press Event History",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                items(sosEventList) { event ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.RadioButtonChecked,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        "Event #${event.eventId}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                    Text(
                                        event.rawPayload,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    if (event.hardwareGpsLocation != null) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(top = 2.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.SatelliteAlt,
                                                contentDescription = null,
                                                tint = SafetyGreen,
                                                modifier = Modifier.size(12.dp)
                                            )
                                            Spacer(modifier = Modifier.width(3.dp))
                                            Text(
                                                "NEO-6M: ${String.format(Locale.US, "%.5f", event.hardwareGpsLocation.latitude)}, ${String.format(Locale.US, "%.5f", event.hardwareGpsLocation.longitude)}",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = SafetyGreen
                                            )
                                        }
                                    }
                                }
                            }
                            Text(
                                SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(event.timestamp)),
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Action Buttons
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = {
                            if (!hasPermissions) {
                                permissionLauncher.launch(permissions)
                            } else if (!bleManager.isBluetoothEnabled()) {
                                enableBtLauncher.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
                            } else {
                                com.example.service.BleForegroundService.start(context)
                                bleManager.scanAndConnect()
                            }
                        },
                        enabled = connectionState != BleState.SCANNING && connectionState != BleState.CONNECTING,
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("ble_scan_button")
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (isConnected) "Reconnect" else "Scan Again")
                    }

                    OutlinedButton(
                        onClick = { bleManager.disconnect() },
                        enabled = connectionState != BleState.DISCONNECTED,
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("ble_disconnect_button")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Disconnect")
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun GattItemRow(label: String, value: String, verified: Boolean) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (verified) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = "Verified",
                        tint = SafetyGreen,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        "Verified",
                        fontSize = 11.sp,
                        color = SafetyGreen,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            value,
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun TelemetryRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
