package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import com.example.utils.hasBluetoothAdvertisePermission
import com.example.utils.hasPostNotificationsPermission
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import com.example.ui.components.SettingsItem
import com.example.ui.components.SettingsSwitchItem
import com.example.ui.components.SettingsSection
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: GuardianViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToSecurity: () -> Unit,
    onNavigateToFallDetection: () -> Unit = {},
    onNavigateToAnalytics: () -> Unit = {},
    onNavigateToVoiceSos: () -> Unit = {},
    onNavigateToSafetyTimer: () -> Unit = {},
    onNavigateToMap: () -> Unit = {},
    onNavigateToQRCode: () -> Unit = {},
    onNavigateToHelpFaq: () -> Unit = {},
    onNavigateToAiScreen: () -> Unit = {},
    onNavigateToTrustedPlaces: () -> Unit = {},
    onNavigateToPermissions: () -> Unit = {},
    onNavigateToAbout: () -> Unit = {},
    onNavigateToDeveloperDashboard: () -> Unit = {}
) {
    val themeMode by viewModel.themeMode.collectAsState()
    val highContrast by viewModel.highContrast.collectAsState()
    val developerModeEnabled by viewModel.developerModeEnabled.collectAsState()
    val language by viewModel.language.collectAsState()
    val notificationsEnabled by viewModel.criticalAlarmsEnabled.collectAsState()
    val voiceSosEnabled by viewModel.voiceSosEnabled.collectAsState()
    val voiceState by viewModel.voiceSosService.voiceState.collectAsState()
    val isSpeechActive by viewModel.voiceSosService.isSpeechRecognizerActive.collectAsState()
    val wakePhrases by viewModel.voiceSosService.wakePhrases.collectAsState()

    val voiceSosPhrase by viewModel.voiceSosPhrase.collectAsState()
    
    val context = androidx.compose.ui.platform.LocalContext.current
    val prefs = context.getSharedPreferences("smart_sos_settings", android.content.Context.MODE_PRIVATE)
    var nearbyPresenceInterval by remember { mutableStateOf(prefs.getInt("nearby_presence_interval", 0)) }
    var nearbyDeviceName by remember {
        mutableStateOf(prefs.getString("nearby_device_name", com.example.ble.nearby.NearbyBleProtocol.DEFAULT_DEVICE_NAME) ?: com.example.ble.nearby.NearbyBleProtocol.DEFAULT_DEVICE_NAME)
    }

    var showVoicePhraseDialog by remember { mutableStateOf(false) }
    var tempPhrase by remember { mutableStateOf("") }
    val sosSoundEnabled by viewModel.sosSoundEnabled.collectAsState()
    val sosVibrationEnabled by viewModel.sosVibrationEnabled.collectAsState()
    
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showDeveloperWarningDialog by remember { mutableStateOf(false) }

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
            prefs.edit().putInt("nearby_presence_interval", nearbyPresenceInterval).apply()
            com.example.service.NearbyBleService.startOrStop(context)
        } else {
            // Permission denied, fail gracefully (do not apply interval, it remains at current)
            pendingNearbyInterval = null
        }
    }


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
                        subtitle = "Manage emergency PIN and biometric login",
                        onClick = onNavigateToSecurity
                    )
                    SettingsItem(
                        icon = Icons.Default.Place,
                        title = "Trusted Places",
                        subtitle = "Manage your safe zones",
                        onClick = onNavigateToTrustedPlaces
                    )
                    SettingsItem(
                        icon = Icons.Default.QrCode,
                        title = "Medical QR Code",
                        subtitle = "View and share your medical information",
                        onClick = onNavigateToQRCode
                    )
                }
            }


            item {
                SettingsSection(title = "Nearby Emergency Presence") {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        androidx.compose.material3.OutlinedTextField(
                            value = nearbyDeviceName,
                            onValueChange = { newName ->
                                nearbyDeviceName = newName
                                prefs.edit().putString("nearby_device_name", newName).apply()
                            },
                            label = { Text("Nearby Device Name") },
                            placeholder = { Text(com.example.ble.nearby.NearbyBleProtocol.DEFAULT_DEVICE_NAME) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "This name is visible to nearby Smart SOS users.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    val presenceOptions = listOf(0, 5, 10, 30, 60)
                    val presenceLabels = mapOf(0 to "Off", 5 to "5 seconds", 10 to "10 seconds", 30 to "30 seconds", 60 to "60 seconds")
                    
                    SettingsItem(
                        icon = Icons.Default.WifiTethering,
                        title = "Nearby Presence (BLE)",
                        subtitle = "Frequency: " + (presenceLabels[nearbyPresenceInterval] ?: "Off"),
                        onClick = {
                            val currentIndex = presenceOptions.indexOf(nearbyPresenceInterval)
                            val nextIndex = (currentIndex + 1) % presenceOptions.size
                            val nextVal = presenceOptions[nextIndex]
                            
                            if (nextVal == 0) {
                                nearbyPresenceInterval = nextVal
                                prefs.edit().putInt("nearby_presence_interval", nextVal).apply()
                                com.example.service.NearbyBleService.startOrStop(context)
                            } else {
                                val hasAdvertise = context.hasBluetoothAdvertisePermission()
                                val hasNotification = context.hasPostNotificationsPermission()

                                if (hasAdvertise && hasNotification) {
                                    nearbyPresenceInterval = nextVal
                                    prefs.edit().putInt("nearby_presence_interval", nextVal).apply()
                                    com.example.service.NearbyBleService.startOrStop(context)
                                } else {
                                    pendingNearbyInterval = nextVal
                                    nearbyPermissionLauncher.launch(nearbyPermissions.toTypedArray())
                                }
                            }
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
                    SettingsItem(
                        icon = Icons.Default.Map,
                        title = "Live Tracking Map",
                        subtitle = "View current location and responders",
                        onClick = onNavigateToMap
                    )
                    SettingsItem(
                        icon = Icons.Default.SmartToy,
                        title = "AI Assistant",
                        subtitle = "Interact with AI Emergency Dashboard",
                        onClick = onNavigateToAiScreen
                    )
                }
            }

            item {
                SettingsSection(title = "Device Settings") {
                    SettingsItem(
                        icon = Icons.AutoMirrored.Filled.DirectionsRun,
                        title = "Fall Detection Calibration",
                        subtitle = "Configure sensitivity for MPU6050",
                        onClick = onNavigateToFallDetection
                    )
                    SettingsItem(
                        icon = Icons.Default.Lock,
                        title = "Permissions",
                        subtitle = "Manage app permissions",
                        onClick = onNavigateToPermissions
                    )
                    SettingsItem(
                        icon = Icons.Default.Analytics,
                        title = "System Analytics",
                        subtitle = "View battery health and connectivity stats",
                        onClick = onNavigateToAnalytics
                    )
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
                        icon = Icons.Default.Language,
                        title = "Language",
                        subtitle = when(language) { "en" -> "English (US)"; "es" -> "Español"; "fr" -> "Français"; else -> language },
                        onClick = { showLanguageDialog = true }
                    )
                    
                    SettingsSwitchItem(
                        icon = Icons.Default.Build,
                        title = "Developer Mode",
                        subtitle = "Enable advanced testing tools",
                        checked = developerModeEnabled,
                        onCheckedChange = { enabled -> 
                            if (enabled) {
                                showDeveloperWarningDialog = true
                            } else {
                                viewModel.setDeveloperModeEnabled(false)
                            }
                        }
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
                                IconButton(onClick = { viewModel.voiceSosService.removeWakePhrase(phrase) }) {
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
                        viewModel.voiceSosService.addWakePhrase(tempPhrase.trim())
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
    if (showLanguageDialog) {
        AlertDialog(
            onDismissRequest = { showLanguageDialog = false },
            title = { Text("Select Language") },
            text = {
                Column {
                    listOf("en" to "English (US)", "es" to "Español", "fr" to "Français").forEach { (code, name) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.setLanguage(code)
                                    showLanguageDialog = false
                                }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = language == code,
                                onClick = {
                                    viewModel.setLanguage(code)
                                    showLanguageDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(name)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLanguageDialog = false }) { Text("Close") }
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
    if (showDeveloperWarningDialog) {
        AlertDialog(
            onDismissRequest = { showDeveloperWarningDialog = false },
            title = { Text("Developer Mode") },
            text = { Text("Warning: Developer mode is intended for testing purposes only and may affect app stability.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.setDeveloperModeEnabled(true)
                        showDeveloperWarningDialog = false
                    }
                ) {
                    Text("Enable")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeveloperWarningDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
