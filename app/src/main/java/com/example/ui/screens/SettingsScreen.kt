package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.GuardianViewModel
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: GuardianViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToSecurity: () -> Unit,
    onNavigateToFallDetection: () -> Unit = {},
    onNavigateToVoiceSos: () -> Unit = {},
    onNavigateToAiScreen: () -> Unit = {}
) {
    val context = LocalContext.current

    // Observe ViewModel Setting States
    val themeMode by viewModel.themeMode.collectAsState()
    val language by viewModel.language.collectAsState()
    val criticalAlarms by viewModel.criticalAlarmsEnabled.collectAsState()
    val arrivalAlerts by viewModel.arrivalAlertsEnabled.collectAsState()
    val deviceStatusNotifications by viewModel.deviceStatusNotificationsEnabled.collectAsState()
    val locationSharingInterval by viewModel.locationSharingInterval.collectAsState()
    val backgroundLocation by viewModel.backgroundLocationEnabled.collectAsState()
    val telemetrySharing by viewModel.telemetrySharingEnabled.collectAsState()
    val isBackupRunning by viewModel.isBackupRunning.collectAsState()
    val lastBackupTime by viewModel.lastBackupTime.collectAsState()

    // Dialog state controllers
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showTermsDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }

    val languages = listOf(
        "en" to "English (US)",
        "es" to "Español (Spanish)",
        "fr" to "Français (French)",
        "de" to "Deutsch (German)",
        "ar" to "العربية (Arabic)"
    )

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Preferences",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "System Customization",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("settings_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Go Back",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
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

            // Category: Appearance & Languages
            item {
                SettingsCategoryHeader(title = "Appearance & Language")
            }

            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
                ) {
                    Column {
                        // Dark Mode Toggle Row
                        SettingsToggleRow(
                            title = "Force Dark Theme",
                            subtitle = "Override system color space and lock dark interface",
                            icon = Icons.Default.DarkMode,
                            iconTint = Color(0xFF9C27B0),
                            checked = themeMode == "DARK",
                            onCheckedChange = { isDark ->
                                viewModel.setThemeMode(if (isDark) "DARK" else "LIGHT")
                            },
                            testTag = "settings_dark_mode_switch"
                        )

                        Divider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f)
                        )

                        // System Theme Sync Row
                        SettingsToggleRow(
                            title = "Sync with Android OS",
                            subtitle = "Adapt interface dynamically based on system color scheduler",
                            icon = Icons.Default.BrightnessAuto,
                            iconTint = Color(0xFF009688),
                            checked = themeMode == "SYSTEM",
                            onCheckedChange = { isSystem ->
                                viewModel.setThemeMode(if (isSystem) "SYSTEM" else "LIGHT")
                            },
                            testTag = "settings_sync_system_switch"
                        )

                        Divider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f)
                        )

                        // Language Selector Card Row
                        SettingsNavigationRow(
                            title = "App Language",
                            subtitle = languages.find { it.first == language }?.second ?: "English (US)",
                            icon = Icons.Default.Translate,
                            iconTint = Color(0xFF3F51B5),
                            onClick = { showLanguageDialog = true },
                            testTag = "settings_language_button"
                        )
                    }
                }
            }

            // Category: Security Center Quick Link
            item {
                SettingsCategoryHeader(title = "Privacy & Encryption Guard")
            }

            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateToSecurity() }
                        .border(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
                        .testTag("settings_navigate_security_card")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Security & PIN Lock",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Password modification, app lock PIN, emergency silent duress trigger, biometric login configuration",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            // Category: Notification Preference
            item {
                SettingsCategoryHeader(title = "Incident Notification Rules")
            }

            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
                ) {
                    Column {
                        SettingsToggleRow(
                            title = "Critical Emergency Alarms",
                            subtitle = "Immediate override of vibration and acoustic limits during active SOS cycles",
                            icon = Icons.Default.Emergency,
                            iconTint = MaterialTheme.colorScheme.error,
                            checked = criticalAlarms,
                            onCheckedChange = { viewModel.setCriticalAlarmsEnabled(it) },
                            testTag = "settings_critical_alarms_switch"
                        )

                        Divider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f)
                        )

                        SettingsToggleRow(
                            title = "Geofence & Arrival Broadcasts",
                            subtitle = "Instant alert dispatch when entering or leaving safe zone beacons",
                            icon = Icons.Default.MyLocation,
                            iconTint = SafetyGreen,
                            checked = arrivalAlerts,
                            onCheckedChange = { viewModel.setArrivalAlertsEnabled(it) },
                            testTag = "settings_arrival_alerts_switch"
                        )

                        Divider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f)
                        )

                        SettingsToggleRow(
                            title = "Device Health Telemetry Status",
                            subtitle = "Warn when ESP32 battery falls below critical limits or Bluetooth is disconnected",
                            icon = Icons.Default.Watch,
                            iconTint = SafetyBlue,
                            checked = deviceStatusNotifications,
                            onCheckedChange = { viewModel.setDeviceStatusNotificationsEnabled(it) },
                            testTag = "settings_device_notifications_switch"
                        )
                    }
                }
            }

            // Category: Hardware Detection Modules
            item {
                SettingsCategoryHeader(title = "Hardware Detection Modules")
            }

            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
                ) {
                    Column {
                        SettingsNavigationRow(
                            title = "Fall Detection Engine (Room)",
                            subtitle = "Local MPU6050 gait classification and automated 15-second countdown settings",
                            icon = Icons.Default.DirectionsRun,
                            iconTint = Color(0xFFEF4444),
                            onClick = onNavigateToFallDetection,
                            testTag = "settings_fall_detection_row"
                        )

                        Divider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f)
                        )

                        SettingsNavigationRow(
                            title = "Voice SOS Activation (Acoustics)",
                            subtitle = "Offline wake word custom triggers, noise thresholding and confidence guards",
                            icon = Icons.Default.Mic,
                            iconTint = Color(0xFF3B82F6),
                            onClick = onNavigateToVoiceSos,
                            testTag = "settings_voice_sos_row"
                        )

                        Divider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f)
                        )

                        SettingsNavigationRow(
                            title = "AI Incident Analyser",
                            subtitle = "Real-time MPU6050 accelerometer graph visualization and neural timeline diagnostic logs",
                            icon = Icons.Default.Troubleshoot,
                            iconTint = Color(0xFF8B5CF6),
                            onClick = onNavigateToAiScreen,
                            testTag = "settings_ai_screen_row"
                        )
                    }
                }
            }

            // Category: Telemetry & Privacy settings
            item {
                SettingsCategoryHeader(title = "Location telemetry & background privacy")
            }

            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
                ) {
                    Column(modifier = Modifier.padding(bottom = 8.dp)) {
                        // Slider / Selector for location frequency
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Timeline,
                                        contentDescription = null,
                                        tint = Color(0xFFFF9800),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = "Location Polling Rate",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.primaryContainer)
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = when (locationSharingInterval) {
                                            "5s" -> "Ultra Fast (5s)"
                                            "10s" -> "Standard (10s)"
                                            "30s" -> "Power Saver (30s)"
                                            "60s" -> "Eco Mode (60s)"
                                            else -> "Manual Sync"
                                        },
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Adjust frequency of dynamic GPS telemetry synchronization with firestore services.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            // Choice Chips
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf("5s", "10s", "30s", "60s").forEach { interval ->
                                    val isSelected = interval == locationSharingInterval
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(
                                                if (isSelected) MaterialTheme.colorScheme.primary
                                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                            )
                                            .clickable { viewModel.setLocationSharingInterval(interval) }
                                            .padding(vertical = 8.dp)
                                            .testTag("settings_interval_$interval"),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = interval,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }

                        Divider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f)
                        )

                        SettingsToggleRow(
                            title = "Background Location Tracing",
                            subtitle = "Enable continuous cell location updates even when app operates in background",
                            icon = Icons.Default.LocationOff,
                            iconTint = Color(0xFFE91E63),
                            checked = backgroundLocation,
                            onCheckedChange = { viewModel.setBackgroundLocationEnabled(it) },
                            testTag = "settings_background_location_switch"
                        )

                        Divider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f)
                        )

                        SettingsToggleRow(
                            title = "Diagnostics Data Telemetry",
                            subtitle = "Anonymously share device temperature, CPU, and network latency logs with cloud support",
                            icon = Icons.Default.CloudQueue,
                            iconTint = Color(0xFF607D8B),
                            checked = telemetrySharing,
                            onCheckedChange = { viewModel.setTelemetrySharingEnabled(it) },
                            testTag = "settings_telemetry_switch"
                        )
                    }
                }
            }

            // Category: Cloud Backup & Restore
            item {
                SettingsCategoryHeader(title = "Disaster Recovery & Backup")
            }

            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Secure Cloud Synchronization",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Last Synced: $lastBackupTime",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.Backup,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Backup emergency contacts, medical passports, hardware bonds, and alarm schedules safely using end-to-end cloud encryption.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        if (isBackupRunning) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                LinearProgressIndicator(
                                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp)),
                                    color = MaterialTheme.colorScheme.primary,
                                    trackColor = MaterialTheme.colorScheme.primaryContainer
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Encrypting database and syncing registry...",
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Button(
                                    onClick = { viewModel.runBackup() },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(44.dp)
                                        .testTag("settings_backup_now_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CloudUpload,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Backup Now", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }

                                Button(
                                    onClick = { viewModel.runRestore() },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(44.dp)
                                        .testTag("settings_restore_now_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CloudDownload,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Restore DB", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            // Category: Support & Legal Info
            item {
                SettingsCategoryHeader(title = "Support & Compliance")
            }

            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
                ) {
                    Column {
                        SettingsNavigationRow(
                            title = "Terms & Privacy Policy",
                            subtitle = "View safety directives, legal agreements, and policy disclosures",
                            icon = Icons.Default.ReceiptLong,
                            iconTint = Color(0xFF8BC34A),
                            onClick = { showTermsDialog = true },
                            testTag = "settings_terms_button"
                        )

                        Divider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f)
                        )

                        SettingsNavigationRow(
                            title = "About Smart SOS App",
                            subtitle = "V12.1.4 • Secure telemetry system details",
                            icon = Icons.Default.Info,
                            iconTint = Color(0xFF607D8B),
                            onClick = { showAboutDialog = true },
                            testTag = "settings_about_button"
                        )
                    }
                }
            }

            // Category: Account & Session
            item {
                SettingsCategoryHeader(title = "Account & Session")
            }

            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
                ) {
                    Column {
                        SettingsNavigationRow(
                            title = "Manage App Security",
                            subtitle = "Update PIN code, biometric parameters, and credentials",
                            icon = Icons.Default.Security,
                            iconTint = Color(0xFF3F51B5),
                            onClick = onNavigateToSecurity,
                            testTag = "settings_security_btn"
                        )

                        Divider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f)
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.logout() }
                                .padding(16.dp)
                                .testTag("settings_logout_row"),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(Color(0xFFE91E63).copy(alpha = 0.12f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ExitToApp,
                                    contentDescription = null,
                                    tint = Color(0xFFE91E63),
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            Column {
                                Text(
                                    text = "Log Out Session",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.error
                                )
                                Text(
                                    text = "Safely clear local session authentication tokens",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }

    // Language picker Dialog
    if (showLanguageDialog) {
        AlertDialog(
            onDismissRequest = { showLanguageDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Translate,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Select App Language")
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    languages.forEach { lang ->
                        val isSelected = lang.first == language
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                    else Color.Transparent
                                )
                                .clickable {
                                    viewModel.setLanguage(lang.first)
                                    showLanguageDialog = false
                                }
                                .padding(horizontal = 12.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = lang.second,
                                fontSize = 14.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                            )
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showLanguageDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Terms and Privacy Dialog
    if (showTermsDialog) {
        AlertDialog(
            onDismissRequest = { showTermsDialog = false },
            title = { Text("Terms & Privacy Directives") },
            text = {
                Column(
                    modifier = Modifier
                        .height(300.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "1. Acceptance of Terms",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "The Guardian application provides real-time emergency telemetry monitoring. By establishing BLE bonds or cellular coordinates sharing, you acknowledge that performance is subject to cellular network limits and satellite lock availability.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Text(
                        text = "2. Data Privacy & Firestore Security",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Your geolocation vectors, contact registry, and health telemetry indexes are stored securely using cloud-hosted instances. Location polling registers only while active tracing is triggered or during background intervals if manually permitted.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Text(
                        text = "3. Emergency Operations Disclaimer",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Guardian is an assistive emergency monitoring node. It does not replace national emergency dispatcher services (911/112). Always contact public security agencies directly in life-threatening scenarios.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { showTermsDialog = false },
                    modifier = Modifier.testTag("settings_terms_agree")
                ) {
                    Text("I Understand")
                }
            }
        )
    }

    // About Dialog
    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            title = { Text("About Smart SOS App") },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Smart SOS Safety System",
                        fontWeight = FontWeight.Black,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Version 12.1.4 (Build 89042)",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "A fully self-contained smart companion for physical security, geofencing, AI telemetry diagnosis, and emergency response.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { showAboutDialog = false },
                    modifier = Modifier.testTag("settings_about_dismiss")
                ) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
fun SettingsCategoryHeader(title: String) {
    Text(
        text = title.uppercase(),
        fontSize = 11.sp,
        fontWeight = FontWeight.ExtraBold,
        color = MaterialTheme.colorScheme.primary,
        letterSpacing = 1.sp,
        modifier = Modifier.padding(start = 8.dp, top = 8.dp)
    )
}

@Composable
fun SettingsToggleRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconTint: Color,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(iconTint.copy(alpha = 0.12f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(18.dp)
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 14.sp
            )
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.testTag(testTag),
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                checkedTrackColor = MaterialTheme.colorScheme.primary
            )
        )
    }
}

@Composable
fun SettingsNavigationRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconTint: Color,
    onClick: () -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(iconTint.copy(alpha = 0.12f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(18.dp)
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            modifier = Modifier.size(20.dp)
        )
    }
}
