package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import com.example.utils.hasBluetoothAdvertisePermission
import com.example.utils.hasPostNotificationsPermission
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import android.os.Build
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.GuardianViewModel
import com.example.repository.SettingsRepository
import com.example.ui.components.SettingsItem
import com.example.ui.components.SettingsSwitchItem
import com.example.ui.components.SettingsSection
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: GuardianViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToSecurity: () -> Unit,
    onNavigateToVoiceSos: () -> Unit = {},
    onNavigateToSafetyTimer: () -> Unit = {},
    onNavigateToHelpFaq: () -> Unit = {},
    onNavigateToTrustedPlaces: () -> Unit = {},
    onNavigateToPermissions: () -> Unit = {},
    onNavigateToAbout: () -> Unit = {}
) {
    val themeMode by viewModel.themeMode.collectAsState()
    val highContrast by viewModel.highContrast.collectAsState()
    val notificationsEnabled by viewModel.criticalAlarmsEnabled.collectAsState()
    val voiceSosEnabled by viewModel.voiceSosEnabled.collectAsState()
    val voiceState by viewModel.voiceSosService.voiceState.collectAsState()
    val isSpeechActive by viewModel.voiceSosService.isSpeechRecognizerActive.collectAsState()
    val wakePhrases by viewModel.voiceSosService.wakePhrases.collectAsState()

    val voiceSosPhrase by viewModel.voiceSosPhrase.collectAsState()
    
    val context = androidx.compose.ui.platform.LocalContext.current
    val settingsRepository = remember { SettingsRepository(context) }
    var fallResponseTime by remember { mutableStateOf(settingsRepository.getFallResponseDelaySeconds()) }
    var showFallResponseTimeDialog by remember { mutableStateOf(false) }

    var physicalSosCancellationWindow by remember { mutableStateOf(settingsRepository.getPhysicalSosCancellationWindowSeconds()) }
    var showPhysicalSosWindowDialog by remember { mutableStateOf(false) }

    var inAppSosActivationDelay by remember { mutableStateOf(settingsRepository.getInAppSosActivationDelaySeconds()) }
    var showInAppSosDelayDialog by remember { mutableStateOf(false) }

    val prefs = context.getSharedPreferences("smart_sos_settings", android.content.Context.MODE_PRIVATE)
    var nearbyPresenceInterval by remember { mutableStateOf(settingsRepository.getNearbyPresenceInterval()) }
    var showNearbyPresenceDialog by remember { mutableStateOf(false) }
    var nearbyDeviceName by remember {
        mutableStateOf(prefs.getString("nearby_device_name", com.example.ble.nearby.NearbyBleProtocol.DEFAULT_DEVICE_NAME) ?: com.example.ble.nearby.NearbyBleProtocol.DEFAULT_DEVICE_NAME)
    }
    var showDeviceNameDialog by remember { mutableStateOf(false) }
    var tempDeviceName by remember { mutableStateOf("") }
    var showNearbyDiscoveryScreen by remember { mutableStateOf(false) }

    var showVoicePhraseDialog by remember { mutableStateOf(false) }
    var tempPhrase by remember { mutableStateOf("") }
    val sosSoundEnabled by viewModel.sosSoundEnabled.collectAsState()
    val sosVibrationEnabled by viewModel.sosVibrationEnabled.collectAsState()
    val emergencySoundId by viewModel.emergencySoundId.collectAsState()
    val emergencyCustomSoundName by viewModel.emergencyCustomSoundName.collectAsState()
    val fallDetectionEnabled by viewModel.fallDetectionEnabled.collectAsState()
    val devices by viewModel.devices.collectAsState()
    val isBleConnected = devices.any { it.status == "CONNECTED" || it.status == "ALERTing" }
    val maxBattery = devices.filter { it.status == "CONNECTED" || it.status == "ALERTing" }.maxOfOrNull { it.batteryLevel } ?: 0
    
    var showLogoutDialog by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()
    var previewPlayer by remember { mutableStateOf<android.media.MediaPlayer?>(null) }
    var isPreviewPlaying by remember { mutableStateOf(false) }

    val stopPreview = {
        try {
            previewPlayer?.stop()
            previewPlayer?.release()
        } catch (_: Exception) {}
        previewPlayer = null
        isPreviewPlaying = false
    }

    val startPreview: (String) -> Unit = { soundId ->
        stopPreview()
        try {
            val audioAttributes = android.media.AudioAttributes.Builder()
                .setUsage(android.media.AudioAttributes.USAGE_MEDIA)
                .setContentType(android.media.AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()

            val player = when (soundId) {
                "builtin_rapid_alarm" -> android.media.MediaPlayer.create(context, com.example.R.raw.emergency_rapid_alarm)
                "builtin_warning_pulse" -> android.media.MediaPlayer.create(context, com.example.R.raw.emergency_warning_pulse)
                "builtin_double_beep" -> android.media.MediaPlayer.create(context, com.example.R.raw.emergency_double_beep)
                "builtin_critical_alert" -> android.media.MediaPlayer.create(context, com.example.R.raw.emergency_critical_alert)
                "builtin_evacuation_tone" -> android.media.MediaPlayer.create(context, com.example.R.raw.emergency_evacuation_tone)
                "custom" -> {
                    val customUriStr = prefs.getString("emergency_custom_sound_uri", null)
                    if (!customUriStr.isNullOrBlank()) {
                        try {
                            android.media.MediaPlayer().apply {
                                setAudioAttributes(audioAttributes)
                                setDataSource(context, android.net.Uri.parse(customUriStr))
                                prepare()
                            }
                        } catch (e: Exception) {
                            android.util.Log.w("SettingsScreen", "Failed to preview custom sound, falling back to siren: ${e.message}")
                            android.media.MediaPlayer.create(context, com.example.R.raw.sos_emergency_siren)
                        }
                    } else {
                        android.media.MediaPlayer.create(context, com.example.R.raw.sos_emergency_siren)
                    }
                }
                else -> android.media.MediaPlayer.create(context, com.example.R.raw.sos_emergency_siren)
            }

            player?.let { p ->
                p.setOnCompletionListener {
                    stopPreview()
                }
                p.start()
                previewPlayer = p
                isPreviewPlaying = true

                coroutineScope.launch {
                    kotlinx.coroutines.delay(4000)
                    if (previewPlayer == p) {
                        stopPreview()
                    }
                }
            } ?: run {
                android.widget.Toast.makeText(context, "Failed to load audio preview", android.widget.Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            android.util.Log.e("SettingsScreen", "Preview playback failed: ${e.message}")
            stopPreview()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            stopPreview()
        }
    }

    val customSoundPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: android.net.Uri? ->
        if (uri != null) {
            try {
                try {
                    context.contentResolver.takePersistableUriPermission(
                        uri,
                        android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                } catch (_: Exception) {}

                var canOpen = false
                try {
                    context.contentResolver.openInputStream(uri)?.use {
                        canOpen = true
                    }
                } catch (_: Exception) {}

                if (canOpen) {
                    var displayName: String? = null
                    try {
                        context.contentResolver.query(uri, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                            if (cursor.moveToFirst()) {
                                val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                                if (nameIndex != -1) {
                                    displayName = cursor.getString(nameIndex)
                                }
                            }
                        }
                    } catch (_: Exception) {}

                    val resolvedName = displayName ?: uri.lastPathSegment ?: "Custom Sound"
                    viewModel.setEmergencyCustomSound(uri.toString(), resolvedName)
                    android.widget.Toast.makeText(context, "Selected: $resolvedName", android.widget.Toast.LENGTH_SHORT).show()
                } else {
                    android.widget.Toast.makeText(context, "Cannot read selected audio file. Keeping previous sound.", android.widget.Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                android.widget.Toast.makeText(context, "Error importing audio: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
            }
        }
    }

    val soundOptions = listOf(
        "builtin_siren" to "Emergency Siren",
        "builtin_rapid_alarm" to "Rapid Alarm",
        "builtin_warning_pulse" to "Warning Pulse",
        "builtin_double_beep" to "Double Beep",
        "builtin_critical_alert" to "Critical Alert",
        "builtin_evacuation_tone" to "Evacuation Tone",
        "custom" to "Custom Sound"
    )

    val nearbyPermissions = mutableListOf<String>()
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        nearbyPermissions.add(Manifest.permission.BLUETOOTH_ADVERTISE)
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        nearbyPermissions.add(Manifest.permission.POST_NOTIFICATIONS)
    }

    var pendingNearbyInterval by remember { mutableStateOf<Int?>(null) }
    
    val nearbyPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.entries.all { it.value }
        if (allGranted && pendingNearbyInterval != null) {
            nearbyPresenceInterval = pendingNearbyInterval!!
            settingsRepository.setNearbyPresenceInterval(nearbyPresenceInterval)
            com.example.service.NearbyBleService.startOrStop(context)
        } else {
            // Permission denied, fail gracefully (do not apply interval, it remains at current)
            pendingNearbyInterval = null
        }
    }

    val applyNearbyInterval: (Int) -> Unit = { nextVal ->
        if (nextVal == 0) {
            nearbyPresenceInterval = nextVal
            settingsRepository.setNearbyPresenceInterval(nextVal)
            viewModel.setNearbyPresenceInterval(nextVal)
            com.example.service.NearbyBleService.startOrStop(context)
        } else {
            val hasAdvertise = context.hasBluetoothAdvertisePermission()
            val hasNotification = context.hasPostNotificationsPermission()

            if (hasAdvertise && hasNotification) {
                nearbyPresenceInterval = nextVal
                settingsRepository.setNearbyPresenceInterval(nextVal)
                viewModel.setNearbyPresenceInterval(nextVal)
                com.example.service.NearbyBleService.startOrStop(context)
            } else {
                pendingNearbyInterval = nextVal
                nearbyPermissionLauncher.launch(nearbyPermissions.toTypedArray())
            }
        }
    }


    if (showNearbyDiscoveryScreen) {
        BackHandler { showNearbyDiscoveryScreen = false }
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text("Nearby Discovery", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = { showNearbyDiscoveryScreen = false }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )
            },
            containerColor = MaterialTheme.colorScheme.background
        ) { paddingValues ->
            val presenceLabels = mapOf(
                0 to "Off",
                5 to "5 seconds",
                10 to "10 seconds",
                30 to "30 seconds",
                60 to "60 seconds"
            )
            val currentPresenceText = presenceLabels[nearbyPresenceInterval] ?: "Off"

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    SettingsSection(title = "Nearby Discovery") {
                        SettingsItem(
                            icon = Icons.Default.PhoneAndroid,
                            title = "Device Name",
                            subtitle = nearbyDeviceName,
                            onClick = {
                                tempDeviceName = nearbyDeviceName
                                showDeviceNameDialog = true
                            }
                        )
                        SettingsItem(
                            icon = Icons.Default.Timer,
                            title = "Frequency",
                            subtitle = currentPresenceText,
                            onClick = {
                                showNearbyPresenceDialog = true
                            }
                        )
                    }
                }
            }
        }
    } else {
        Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(modifier = Modifier.height(8.dp)) }
            
            item {
                SettingsSection(title = "Account & Security") {
                    SettingsItem(
                        icon = Icons.Default.Security,
                        title = "Security & PIN",
                        subtitle = "Manage emergency duress PIN and account credentials",
                        onClick = onNavigateToSecurity
                    )
                    SettingsItem(
                        icon = Icons.Default.Place,
                        title = "Trusted Places",
                        subtitle = "Manage your safe zones",
                        onClick = onNavigateToTrustedPlaces
                    )
                }
            }


            item {
                SettingsSection(title = "Nearby Discovery") {
                    val presenceLabels = mapOf(
                        0 to "Off",
                        5 to "5 seconds",
                        10 to "10 seconds",
                        30 to "30 seconds",
                        60 to "60 seconds"
                    )
                    val currentPresenceText = presenceLabels[nearbyPresenceInterval] ?: "Off"

                    SettingsItem(
                        icon = Icons.Default.WifiTethering,
                        title = "Nearby Discovery",
                        subtitle = "$nearbyDeviceName • $currentPresenceText",
                        onClick = {
                            showNearbyDiscoveryScreen = true
                        }
                    )
                }
            }

            item {
                SettingsSection(title = "Voice SOS & Emergency Phrases") {
                    SettingsSwitchItem(
                        icon = Icons.Default.Mic,
                        title = "Voice SOS",
                        subtitle = if (voiceSosEnabled) (if (isSpeechActive) "Listening: Active" else "Listening: Inactive") else "Disabled",
                        checked = voiceSosEnabled,
                        onCheckedChange = { viewModel.setVoiceSosEnabled(it) }
                    )
                    if (voiceSosEnabled) {
                        SettingsItem(
                            icon = Icons.AutoMirrored.Filled.FormatListBulleted,
                            title = "Configured Phrases (${wakePhrases.size})",
                            subtitle = wakePhrases.take(3).joinToString(", ") + if (wakePhrases.size > 3) "..." else "",
                            onClick = { 
                                tempPhrase = ""
                                showVoicePhraseDialog = true 
                            }
                        )
                    }
                }
            }
            item {
                SettingsSection(title = "Features") {
                    SettingsItem(
                        icon = Icons.Default.Timer,
                        title = "Safety Timer",
                        subtitle = "Set up countdown safety timers",
                        onClick = onNavigateToSafetyTimer
                    )
                }
            }

            item {
                SettingsSection(title = "SOS Timing & Cancellation") {
                    SettingsItem(
                        icon = Icons.Default.RadioButtonChecked,
                        title = "Physical SOS Cancellation Window",
                        subtitle = "Time available to press the physical SOS button again to cancel the SOS.\n$physicalSosCancellationWindow seconds",
                        onClick = { showPhysicalSosWindowDialog = true }
                    )
                    SettingsItem(
                        icon = Icons.Default.HourglassTop,
                        title = "In-App SOS Activation Delay",
                        subtitle = "Time to wait before starting the emergency workflow after pressing in-app SOS.\n${if (inAppSosActivationDelay == 0) "Immediate" else "$inAppSosActivationDelay seconds"}",
                        onClick = { showInAppSosDelayDialog = true }
                    )
                }
            }

            item {
                SettingsSection(title = "Device Settings") {
                    SettingsSwitchItem(
                        icon = Icons.AutoMirrored.Filled.DirectionsRun,
                        title = "Fall Detection",
                        subtitle = if (fallDetectionEnabled) "Automatic fall detection enabled via MPU6050" else "Fall detection disabled",
                        checked = fallDetectionEnabled,
                        onCheckedChange = { enabled -> viewModel.setFallDetectionEnabled(enabled) }
                    )
                    SettingsItem(
                        icon = Icons.Default.Timer,
                        title = "Fall Response Time",
                        subtitle = "Time before a detected fall starts the emergency workflow\n$fallResponseTime seconds",
                        onClick = { showFallResponseTimeDialog = true }
                    )
                    SettingsItem(
                        icon = Icons.Default.Lock,
                        title = "Permissions",
                        subtitle = "Manage app permissions",
                        onClick = onNavigateToPermissions
                    )
                }
            }

            item {
                SettingsSection(title = "Emergency Sound") {
                    val selectedSoundName = when (emergencySoundId) {
                        "builtin_rapid_alarm" -> "Rapid Alarm"
                        "builtin_warning_pulse" -> "Warning Pulse"
                        "builtin_double_beep" -> "Double Beep"
                        "builtin_critical_alert" -> "Critical Alert"
                        "builtin_evacuation_tone" -> "Evacuation Tone"
                        "custom" -> if (!emergencyCustomSoundName.isNullOrBlank()) "Custom: $emergencyCustomSoundName" else "Custom Sound"
                        else -> "Emergency Siren"
                    }

                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Choose the sound played when an emergency starts.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        soundOptions.forEach { (id, title) ->
                            val isSelected = emergencySoundId == id
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clickable {
                                        if (id == "custom" && prefs.getString("emergency_custom_sound_uri", null).isNullOrBlank()) {
                                            customSoundPickerLauncher.launch(arrayOf("audio/*"))
                                        } else {
                                            viewModel.setEmergencySoundId(id)
                                        }
                                    },
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f) else Color.Transparent,
                                tonalElevation = if (isSelected) 2.dp else 0.dp
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = isSelected,
                                        onClick = {
                                            if (id == "custom" && prefs.getString("emergency_custom_sound_uri", null).isNullOrBlank()) {
                                                customSoundPickerLauncher.launch(arrayOf("audio/*"))
                                            } else {
                                                viewModel.setEmergencySoundId(id)
                                            }
                                        }
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = title,
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        if (id == "custom") {
                                            val customName = emergencyCustomSoundName
                                            Text(
                                                text = if (!customName.isNullOrBlank()) customName else "No custom sound selected.",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = if (!customName.isNullOrBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    if (id == "custom") {
                                        OutlinedButton(
                                            onClick = { customSoundPickerLauncher.launch(arrayOf("audio/*")) },
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text(
                                                text = if (!emergencyCustomSoundName.isNullOrBlank()) "Change" else "Import from Phone",
                                                style = MaterialTheme.typography.labelMedium
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Current: $selectedSoundName",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            OutlinedButton(
                                onClick = {
                                    if (isPreviewPlaying) {
                                        stopPreview()
                                    } else {
                                        startPreview(emergencySoundId)
                                    }
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = if (isPreviewPlaying) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Icon(
                                    imageVector = if (isPreviewPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                                    contentDescription = if (isPreviewPlaying) "Stop Preview" else "Preview Sound",
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (isPreviewPlaying) "Stop" else "Preview")
                            }
                        }
                    }
                }
            }

            item {
                SettingsSection(title = "Preferences") {
                    SettingsSwitchItem(
                        icon = if (sosSoundEnabled) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeOff,
                        title = "SOS Trigger Sound",
                        subtitle = if (sosSoundEnabled) "Sound siren automatically when SOS triggers" else "Silent SOS mode (alarm sound disabled)",
                        checked = sosSoundEnabled,
                        onCheckedChange = { enabled -> viewModel.setSosSoundEnabled(enabled) }
                    )
                    SettingsSwitchItem(
                        icon = Icons.Default.Vibration,
                        title = "SOS Trigger Vibration",
                        subtitle = if (sosVibrationEnabled) "Vibrate automatically when SOS triggers" else "No vibration when SOS triggers",
                        checked = sosVibrationEnabled,
                        onCheckedChange = { enabled -> viewModel.setSosVibrationEnabled(enabled) }
                    )
                    SettingsSwitchItem(
                        icon = Icons.Default.DarkMode,
                        title = "Dark Theme",
                        subtitle = "Toggle dark mode",
                        checked = themeMode == "DARK",
                        onCheckedChange = { isDark -> viewModel.setThemeMode(if (isDark) "DARK" else "LIGHT") }
                    )
                    SettingsSwitchItem(
                        icon = Icons.Default.Contrast,
                        title = "High Contrast",
                        subtitle = "Increase readability and contrast",
                        checked = highContrast,
                        onCheckedChange = { enabled -> viewModel.setHighContrast(enabled) }
                    )
                    SettingsSwitchItem(
                        icon = Icons.Default.Notifications,
                        title = "Notifications",
                        subtitle = "Alert sounds and haptics",
                        checked = notificationsEnabled,
                        onCheckedChange = { enabled -> viewModel.setCriticalAlarmsEnabled(enabled) }
                    )
                    SettingsItem(
                        icon = Icons.Default.Info,

                        title = "About Smart SOS",
                        subtitle = "Version, Terms, and Privacy",
                        onClick = onNavigateToAbout
                    )
                    SettingsItem(
                        icon = Icons.AutoMirrored.Filled.Help,
                        title = "Help & FAQ",
                        subtitle = "Get support and read FAQs",
                        onClick = onNavigateToHelpFaq
                    )
                }
            }
            
            item {
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = { showLogoutDialog = true },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer, contentColor = MaterialTheme.colorScheme.onErrorContainer),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Log Out", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

    if (showVoicePhraseDialog) {
        AlertDialog(
            onDismissRequest = { showVoicePhraseDialog = false },
            title = { Text("Emergency Phrases") },
            text = {
                Column {
                    Text("Add a new phrase to trigger SOS:", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(16.dp))
                    androidx.compose.material3.OutlinedTextField(
                        value = tempPhrase,
                        onValueChange = { tempPhrase = it },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("New phrase") }
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Current Phrases:", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    androidx.compose.foundation.lazy.LazyColumn(
                        modifier = Modifier.heightIn(max = 200.dp)
                    ) {
                        items(wakePhrases.size) { index ->
                            val phrase = wakePhrases[index]
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(phrase, style = MaterialTheme.typography.bodyMedium)
                                IconButton(onClick = { viewModel.removeWakePhrase(phrase) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (tempPhrase.isNotBlank()) {
                        viewModel.addWakePhrase(tempPhrase.trim())
                        tempPhrase = ""
                    } else {
                        showVoicePhraseDialog = false
                    }
                }) {
                    Text(if (tempPhrase.isNotBlank()) "Add Phrase" else "Done")
                }
            },
            dismissButton = {
                TextButton(onClick = { showVoicePhraseDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    if (showFallResponseTimeDialog) {
        AlertDialog(
            onDismissRequest = { showFallResponseTimeDialog = false },
            title = { Text("Fall Response Time") },
            text = {
                Column {
                    listOf(5, 10, 12, 15, 20, 30).forEach { seconds ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    fallResponseTime = seconds
                                    settingsRepository.setFallResponseDelaySeconds(seconds)
                                    viewModel.setFallResponseDelaySeconds(seconds)
                                    showFallResponseTimeDialog = false
                                }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = fallResponseTime == seconds,
                                onClick = {
                                    fallResponseTime = seconds
                                    settingsRepository.setFallResponseDelaySeconds(seconds)
                                    viewModel.setFallResponseDelaySeconds(seconds)
                                    showFallResponseTimeDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("$seconds seconds")
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showFallResponseTimeDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showPhysicalSosWindowDialog) {
        AlertDialog(
            onDismissRequest = { showPhysicalSosWindowDialog = false },
            title = { Text("Physical SOS Cancellation Window") },
            text = {
                Column {
                    Text(
                        text = "Time available to press the physical SOS button again to cancel the SOS.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    SettingsRepository.AVAILABLE_PHYSICAL_SOS_CANCELLATION_WINDOWS.forEach { seconds ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    physicalSosCancellationWindow = seconds
                                    settingsRepository.setPhysicalSosCancellationWindowSeconds(seconds)
                                    viewModel.setPhysicalSosCancellationWindowSeconds(seconds)
                                    showPhysicalSosWindowDialog = false
                                }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = physicalSosCancellationWindow == seconds,
                                onClick = {
                                    physicalSosCancellationWindow = seconds
                                    settingsRepository.setPhysicalSosCancellationWindowSeconds(seconds)
                                    viewModel.setPhysicalSosCancellationWindowSeconds(seconds)
                                    showPhysicalSosWindowDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("$seconds seconds")
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showPhysicalSosWindowDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showInAppSosDelayDialog) {
        AlertDialog(
            onDismissRequest = { showInAppSosDelayDialog = false },
            title = { Text("In-App SOS Activation Delay") },
            text = {
                Column {
                    Text(
                        text = "Time to wait before starting the emergency workflow after pressing in-app SOS.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    SettingsRepository.AVAILABLE_IN_APP_SOS_ACTIVATION_DELAYS.forEach { seconds ->
                        val label = if (seconds == 0) "Immediate" else "$seconds seconds"
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    inAppSosActivationDelay = seconds
                                    settingsRepository.setInAppSosActivationDelaySeconds(seconds)
                                    viewModel.setInAppSosActivationDelaySeconds(seconds)
                                    showInAppSosDelayDialog = false
                                }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = inAppSosActivationDelay == seconds,
                                onClick = {
                                    inAppSosActivationDelay = seconds
                                    settingsRepository.setInAppSosActivationDelaySeconds(seconds)
                                    viewModel.setInAppSosActivationDelaySeconds(seconds)
                                    showInAppSosDelayDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(label)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showInAppSosDelayDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showDeviceNameDialog) {
        AlertDialog(
            onDismissRequest = { showDeviceNameDialog = false },
            title = {
                Text(
                    text = "Device Name",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Column {
                    Text(
                        text = "This name represents this phone during nearby discovery.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    androidx.compose.material3.OutlinedTextField(
                        value = tempDeviceName,
                        onValueChange = { tempDeviceName = it },
                        label = { Text("Device Name") },
                        placeholder = { Text(com.example.ble.nearby.NearbyBleProtocol.DEFAULT_DEVICE_NAME) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val trimmed = tempDeviceName.trim()
                        val finalName = if (trimmed.isNotEmpty()) trimmed else com.example.ble.nearby.NearbyBleProtocol.DEFAULT_DEVICE_NAME
                        nearbyDeviceName = finalName
                        prefs.edit().putString("nearby_device_name", finalName).apply()
                        viewModel.setNearbyDeviceName(finalName)
                        showDeviceNameDialog = false
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeviceNameDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showNearbyPresenceDialog) {
        val presenceOptions = listOf(0, 5, 10, 30, 60)
        val presenceLabels = mapOf(0 to "Off", 5 to "5 seconds", 10 to "10 seconds", 30 to "30 seconds", 60 to "60 seconds")

        AlertDialog(
            onDismissRequest = { showNearbyPresenceDialog = false },
            title = {
                Text(
                    text = "Nearby Presence",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Column {
                    Text(
                        text = "Controls how often nearby-device presence activity occurs.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    presenceOptions.forEach { interval ->
                        val label = presenceLabels[interval] ?: "$interval seconds"
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    applyNearbyInterval(interval)
                                    showNearbyPresenceDialog = false
                                }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = nearbyPresenceInterval == interval,
                                onClick = {
                                    applyNearbyInterval(interval)
                                    showNearbyPresenceDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            },
            confirmButton = { },
            dismissButton = {
                TextButton(onClick = { showNearbyPresenceDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Log Out") },
            text = { Text("Are you sure you want to log out? Your SOS wearable will remain active but won't sync until you log back in.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.logout()
                        showLogoutDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Log Out")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) { Text("Cancel") }
            }
        )
    }
}
