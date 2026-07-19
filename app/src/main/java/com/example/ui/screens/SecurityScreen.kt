package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.GuardianViewModel
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecurityScreen(
    viewModel: GuardianViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current

    // Observe State flows from ViewModel
    val biometricEnabled by viewModel.biometricEnabled.collectAsState()
    val appLockPinEnabled by viewModel.appLockPinEnabled.collectAsState()
    val appLockPin by viewModel.appLockPin.collectAsState()
    val emergencyPin by viewModel.emergencyPin.collectAsState()

    // Password State
    var oldPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var showOldPassword by remember { mutableStateOf(false) }
    var showNewPassword by remember { mutableStateOf(false) }
    var showConfirmPassword by remember { mutableStateOf(false) }
    var isChangingPassword by remember { mutableStateOf(false) }

    // Dialog control
    var showPinSetupDialog by remember { mutableStateOf(false) }
    var showEmergencyPinDialog by remember { mutableStateOf(false) }
    var showDeleteAccountDialog by remember { mutableStateOf(false) }
    var deleteConfirmText by remember { mutableStateOf("") }

    // Temporary values for dialogs
    var inputPin by remember { mutableStateOf("") }
    var confirmInputPin by remember { mutableStateOf("") }
    var inputEmergencyPin by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Security Center",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "Identity & Encryption Shield",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("security_back_button")
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

            // Section: Password Management
            item {
                SecurityCategoryHeader(title = "Account Password Modification")
            }

            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Update Passphrase",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        // Old Password Field
                        OutlinedTextField(
                            value = oldPassword,
                            onValueChange = { oldPassword = it },
                            label = { Text("Current Password") },
                            singleLine = true,
                            visualTransformation = if (showOldPassword) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                IconButton(onClick = { showOldPassword = !showOldPassword }) {
                                    Icon(
                                        imageVector = if (showOldPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = "Toggle Visibility"
                                    )
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("security_old_password_input")
                        )

                        // New Password Field
                        OutlinedTextField(
                            value = newPassword,
                            onValueChange = { newPassword = it },
                            label = { Text("New Password (Min 4 chars)") },
                            singleLine = true,
                            visualTransformation = if (showNewPassword) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                IconButton(onClick = { showNewPassword = !showNewPassword }) {
                                    Icon(
                                        imageVector = if (showNewPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = "Toggle Visibility"
                                    )
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("security_new_password_input")
                        )

                        // Confirm New Password Field
                        OutlinedTextField(
                            value = confirmPassword,
                            onValueChange = { confirmPassword = it },
                            label = { Text("Confirm New Password") },
                            singleLine = true,
                            visualTransformation = if (showConfirmPassword) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                IconButton(onClick = { showConfirmPassword = !showConfirmPassword }) {
                                    Icon(
                                        imageVector = if (showConfirmPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = "Toggle Visibility"
                                    )
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("security_confirm_password_input")
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        // Change password action button
                        Button(
                            onClick = {
                                if (oldPassword.isBlank() || newPassword.isBlank()) {
                                    // simple trigger toast
                                } else if (newPassword != confirmPassword) {
                                    // simple warning
                                } else {
                                    isChangingPassword = true
                                    viewModel.changePassword(oldPassword, newPassword) { success ->
                                        isChangingPassword = false
                                        if (success) {
                                            oldPassword = ""
                                            newPassword = ""
                                            confirmPassword = ""
                                        }
                                    }
                                }
                            },
                            enabled = !isChangingPassword && oldPassword.isNotBlank() && newPassword.length >= 4 && confirmPassword.isNotBlank(),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("security_change_password_button")
                        ) {
                            if (isChangingPassword) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.VpnKey,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Update Passphrase", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                        }
                    }
                }
            }

            // Section: Biometric & App Lock
            item {
                SecurityCategoryHeader(title = "App Lock & Biometrics")
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
                        // Biometric Lock toggle
                        SettingsToggleRow(
                            title = "Biometric Authentication",
                            subtitle = "Verify fingerprint or facial vectors when opening Guardian",
                            icon = Icons.Default.Fingerprint,
                            iconTint = Color(0xFF00bcd4),
                            checked = biometricEnabled,
                            onCheckedChange = { checked -> viewModel.setBiometricEnabled(checked) },
                            testTag = "security_biometric_switch"
                        )

                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f)
                        )

                        // App Lock PIN setup / toggle row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    inputPin = ""
                                    confirmInputPin = ""
                                    showPinSetupDialog = true
                                }
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(Color(0xFF3F51B5).copy(alpha = 0.12f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = Color(0xFF3F51B5),
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "App Lock PIN Setup",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (appLockPinEnabled) "PIN Secured (Active)" else "PIN Disabled • Tap to configure",
                                    fontSize = 11.sp,
                                    color = if (appLockPinEnabled) SafetyGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = if (appLockPinEnabled) FontWeight.Bold else FontWeight.Normal
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
                }
            }

            // Section: Silent Emergency Duress Trigger
            item {
                SecurityCategoryHeader(title = "Silent duress trigger")
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
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(MaterialTheme.colorScheme.error.copy(alpha = 0.12f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.NotificationImportant,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Column {
                                    Text(
                                        text = "Emergency Duress PIN",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Active Code: $emergencyPin",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.error,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            IconButton(
                                onClick = {
                                    inputEmergencyPin = emergencyPin
                                    showEmergencyPinDialog = true
                                },
                                modifier = Modifier.testTag("security_setup_emergency_pin_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Change Duress PIN",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "If forced to unlock your device or security panels under duress by an intruder, entering this silent PIN will unlock a fake benign dashboard while immediately broadcasting a silent SOS, sharing real-time GPS coords, and disabling all acoustic alarms.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 16.sp
                        )
                    }
                }
            }

            // Section: Session Management
            item {
                SecurityCategoryHeader(title = "Account Administration")
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
                        // Logout row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.logout() }
                                .padding(16.dp)
                                .testTag("security_logout_row"),
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
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Safely clear local session authentication tokens",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f)
                        )

                        // Delete account row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    deleteConfirmText = ""
                                    showDeleteAccountDialog = true
                                }
                                .padding(16.dp)
                                .testTag("security_delete_account_row"),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(MaterialTheme.colorScheme.error.copy(alpha = 0.12f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteForever,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            Column {
                                Text(
                                    text = "Permanently Delete Account",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.error
                                )
                                Text(
                                    text = "Erase contacts history, bonds and passport records permanently",
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

    // PIN Setup Dialog
    if (showPinSetupDialog) {
        AlertDialog(
            onDismissRequest = { showPinSetupDialog = false },
            title = { Text(if (appLockPinEnabled) "Update Lock PIN" else "Configure App Lock PIN") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Specify a 4-digit PIN code to secure access to the Guardian app.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = inputPin,
                        onValueChange = { if (it.length <= 4 && it.all { char -> char.isDigit() }) inputPin = it },
                        label = { Text("Enter 4-Digit PIN") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("security_pin_setup_input_field")
                    )

                    OutlinedTextField(
                        value = confirmInputPin,
                        onValueChange = { if (it.length <= 4 && it.all { char -> char.isDigit() }) confirmInputPin = it },
                        label = { Text("Confirm PIN Code") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("security_pin_setup_confirm_field")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (inputPin.length == 4 && inputPin == confirmInputPin) {
                            viewModel.setAppLockPin(inputPin, true)
                            showPinSetupDialog = false
                        }
                    },
                    enabled = inputPin.length == 4 && inputPin == confirmInputPin,
                    modifier = Modifier.testTag("security_pin_setup_save")
                ) {
                    Text("Enable PIN")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        if (appLockPinEnabled) {
                            viewModel.setAppLockPin("", false)
                        }
                        showPinSetupDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(if (appLockPinEnabled) "Disable PIN" else "Cancel")
                }
            }
        )
    }

    // Emergency Silent PIN Dialog
    if (showEmergencyPinDialog) {
        AlertDialog(
            onDismissRequest = { showEmergencyPinDialog = false },
            title = { Text("Setup Emergency Duress PIN") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Enter a unique 4-digit silent distress PIN. Entering this PIN under threat triggers silent alarms.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = inputEmergencyPin,
                        onValueChange = { if (it.length <= 4 && it.all { char -> char.isDigit() }) inputEmergencyPin = it },
                        label = { Text("Emergency PIN Code") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("security_emergency_pin_input_field")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (inputEmergencyPin.length == 4) {
                            viewModel.setEmergencyPin(inputEmergencyPin)
                            showEmergencyPinDialog = false
                        }
                    },
                    enabled = inputEmergencyPin.length == 4,
                    modifier = Modifier.testTag("security_emergency_pin_save")
                ) {
                    Text("Save Code")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEmergencyPinDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Delete Account Confirmation Dialog
    if (showDeleteAccountDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteAccountDialog = false },
            title = { Text("ERASE ACCOUNT FOREVER?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "WARNING: This action is completely irreversible. All registered emergency guardians, BLE bonds, history, and medical records will be deleted instantly.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "To confirm this absolute erasure, please type \"DELETE\" below:",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = deleteConfirmText,
                        onValueChange = { deleteConfirmText = it },
                        label = { Text("Confirmation Phrase") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("security_delete_account_input_field")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (deleteConfirmText == "DELETE") {
                            viewModel.deleteAccount()
                            showDeleteAccountDialog = false
                        }
                    },
                    enabled = deleteConfirmText == "DELETE",
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.testTag("security_delete_account_confirm")
                ) {
                    Text("DELETE FOREVER")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAccountDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun SecurityCategoryHeader(title: String) {
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
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    iconTint: androidx.compose.ui.graphics.Color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant,
    checked: Boolean,
    testTag: String = "",
    onCheckedChange: (Boolean) -> Unit
) {
    androidx.compose.foundation.layout.Row(
        modifier = androidx.compose.ui.Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween
    ) {
        if (icon != null) {
            androidx.compose.material3.Icon(icon, contentDescription = null, tint = iconTint)
            androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.ui.Modifier.width(16.dp))
        }
        androidx.compose.foundation.layout.Column(modifier = androidx.compose.ui.Modifier.weight(1f)) {
            androidx.compose.material3.Text(title, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold, fontSize = 16.sp)
            androidx.compose.material3.Text(subtitle, color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
        }
        androidx.compose.material3.Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
