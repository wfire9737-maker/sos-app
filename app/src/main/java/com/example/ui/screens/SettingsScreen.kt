package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
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
    onNavigateToAnalytics: () -> Unit = {},
    onNavigateToVoiceSos: () -> Unit = {},
    onNavigateToSafetyTimer: () -> Unit = {},
    onNavigateToMap: () -> Unit = {},
    onNavigateToQRCode: () -> Unit = {},
    onNavigateToHelpFaq: () -> Unit = {},
    onNavigateToAiScreen: () -> Unit = {},
    onNavigateToTrustedPlaces: () -> Unit = {},
    onNavigateToPermissions: () -> Unit = {},
    onNavigateToAbout: () -> Unit = {}
) {
    val themeMode by viewModel.themeMode.collectAsState()
    val language by viewModel.language.collectAsState()
    val notificationsEnabled by viewModel.criticalAlarmsEnabled.collectAsState()
    
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }

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
                SettingsSection(title = "Features") {
                    SettingsItem(
                        icon = Icons.Default.Mic,
                        title = "Voice SOS",
                        subtitle = "Configure voice activation",
                        onClick = onNavigateToVoiceSos
                    )
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
                        icon = Icons.Default.DirectionsRun,
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
                        icon = Icons.Default.DarkMode,
                        title = "Dark Theme",
                        subtitle = "Toggle dark mode",
                        checked = themeMode == "DARK",
                        onCheckedChange = { isDark -> viewModel.setThemeMode(if (isDark) "DARK" else "LIGHT") }
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
                    SettingsItem(
                        icon = Icons.Default.Info,
                        title = "About Smart SOS",
                        subtitle = "Version, Terms, and Privacy",
                        onClick = onNavigateToAbout
                    )
                    SettingsItem(
                        icon = Icons.Default.Help,
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
}

@Composable
fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 8.dp, bottom = 8.dp)
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                content()
            }
        }
    }
}

@Composable
fun SettingsItem(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
        }
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}


@Composable
fun SettingsSwitchItem(icon: ImageVector, title: String, subtitle: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}
