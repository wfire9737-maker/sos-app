package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.model.User
import com.example.service.AuthState
import com.example.ui.GuardianViewModel
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: GuardianViewModel,
    onNavigateBack: () -> Unit
) {
    val authState by viewModel.authState.collectAsState()
    val currentUser = (authState as? AuthState.Success)?.user ?: User(name = "Guardian User")
    val context = LocalContext.current

    // Local input state initialized from current user details
    var name by remember { mutableStateOf(currentUser.name) }
    var phone by remember { mutableStateOf(currentUser.phone) }
    var email by remember { mutableStateOf(currentUser.email) }
    var emergencyContactName by remember { mutableStateOf(currentUser.emergencyContactName) }
    var emergencyContactPhone by remember { mutableStateOf(currentUser.emergencyContactPhone) }
    
    // Structured Medical details
    var bloodType by remember { mutableStateOf(currentUser.bloodType.ifBlank { "O+" }) }
    var allergies by remember { mutableStateOf(currentUser.allergies) }
    var conditions by remember { mutableStateOf(currentUser.conditions) }
    var medications by remember { mutableStateOf(currentUser.medications) }
    var photoUriString by remember { mutableStateOf(currentUser.photoUri) }

    var isSaving by remember { mutableStateOf(false) }
    var bloodDropdownExpanded by remember { mutableStateOf(false) }

    val bloodTypesList = listOf("A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-")

    // File/Image picking activity launcher
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            photoUriString = uri.toString()
        }
    }

    // Set of premium preset responder/guardian avatars for instant choice
    val presetAvatars = listOf(
        "🛡️" to "Guardian",
        "🏥" to "Medic",
        "🦸" to "Responder",
        "🚑" to "EMT",
        "⚙️" to "Dispatcher"
    )

    var showAvatarDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Medical Passport",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = ProfOnBackgroundLight,
                        letterSpacing = (-0.5).sp
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Navigate Back",
                            tint = ProfPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = ProfBackgroundLight
                )
            )
        },
        containerColor = ProfBackgroundLight
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // --- PROFILE PHOTO COLUMN ---
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            contentAlignment = Alignment.BottomEnd,
                            modifier = Modifier.size(120.dp)
                        ) {
                            if (!photoUriString.isNullOrBlank()) {
                                if (photoUriString!!.startsWith("http") || photoUriString!!.startsWith("content://") || photoUriString!!.startsWith("file://")) {
                                    AsyncImage(
                                        model = photoUriString,
                                        contentDescription = "Profile Photo",
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(CircleShape)
                                            .border(3.dp, ProfPrimary, CircleShape)
                                    )
                                } else {
                                    // Preset emoji fallback
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(CircleShape)
                                            .background(ProfPrimaryContainer)
                                            .border(3.dp, ProfPrimary, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(photoUriString!!, fontSize = 48.sp)
                                    }
                                }
                            } else {
                                // Default initials bubble
                                val initials = if (name.isNotBlank()) {
                                    name.split(" ")
                                        .mapNotNull { it.firstOrNull()?.uppercaseChar() }
                                        .joinToString("")
                                        .take(2)
                                } else {
                                    "GU"
                                }
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(CircleShape)
                                        .background(ProfPrimaryContainer)
                                        .border(3.dp, ProfPrimary, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = initials,
                                        fontSize = 32.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ProfOnPrimaryContainer
                                    )
                                }
                            }

                            // Small edit badge button
                            IconButton(
                                onClick = { showAvatarDialog = true },
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(ProfPrimary)
                                    .border(2.dp, Color.White, CircleShape)
                                    .testTag("upload_photo_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CameraAlt,
                                    contentDescription = "Edit Profile Picture",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        
                        Text(
                            text = "Tap camera badge to upload a real photo or select a premium emergency responder emblem.",
                            fontSize = 11.sp,
                            color = ProfOnSurfaceVariantLight,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 32.dp)
                        )
                    }
                }

                // --- PERSONAL IDENTITY CARD ---
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, ProfBorderLight, RoundedCornerShape(20.dp))
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Text(
                                text = "PERSONAL DETAILS",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = ProfPrimary,
                                letterSpacing = 1.sp
                            )

                            // Name Field
                            OutlinedTextField(
                                value = name,
                                onValueChange = { name = it },
                                label = { Text("Full Name") },
                                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = ProfPrimary) },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("profile_name_field"),
                                shape = RoundedCornerShape(12.dp)
                            )

                            // Email Field (Read Only)
                            OutlinedTextField(
                                value = email,
                                onValueChange = {},
                                label = { Text("Email Address (Primary)") },
                                leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = ProfOnSurfaceVariantLight) },
                                singleLine = true,
                                enabled = false,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            )

                            // Phone Field
                            OutlinedTextField(
                                value = phone,
                                onValueChange = { phone = it },
                                label = { Text("Contact Number") },
                                leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = ProfPrimary) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("profile_phone_field"),
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                    }
                }

                // --- EMERGENCY GUARDIAN CONTACT CARD ---
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, ProfBorderLight, RoundedCornerShape(20.dp))
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Text(
                                text = "EMERGENCY CONTACT",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = ProfPrimary,
                                letterSpacing = 1.sp
                            )

                            // Guardian Name
                            OutlinedTextField(
                                value = emergencyContactName,
                                onValueChange = { emergencyContactName = it },
                                label = { Text("Primary Guardian Name") },
                                leadingIcon = { Icon(Icons.Default.Shield, contentDescription = null, tint = ProfPrimary) },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("profile_guardian_name_field"),
                                shape = RoundedCornerShape(12.dp)
                            )

                            // Guardian Phone
                            OutlinedTextField(
                                value = emergencyContactPhone,
                                onValueChange = { emergencyContactPhone = it },
                                label = { Text("Guardian Contact Number") },
                                leadingIcon = { Icon(Icons.Default.ContactPhone, contentDescription = null, tint = ProfPrimary) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("profile_guardian_phone_field"),
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                    }
                }

                // --- DETAILED MEDICAL PASSPORT CARD ---
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, ProfBorderLight, RoundedCornerShape(20.dp))
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Text(
                                text = "VITAL MEDICAL INFORMATION",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = ProfPrimary,
                                letterSpacing = 1.sp
                            )

                            // Blood Type Dropdown Selector
                            Box(modifier = Modifier.fillMaxWidth()) {
                                OutlinedTextField(
                                    value = bloodType,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Blood Type") },
                                    leadingIcon = { Icon(Icons.Default.Bloodtype, contentDescription = null, tint = Color.Red) },
                                    trailingIcon = {
                                        IconButton(onClick = { bloodDropdownExpanded = !bloodDropdownExpanded }) {
                                            Icon(
                                                imageVector = if (bloodDropdownExpanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                                                contentDescription = "Expand Blood Types"
                                            )
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { bloodDropdownExpanded = !bloodDropdownExpanded }
                                        .testTag("blood_type_selector"),
                                    shape = RoundedCornerShape(12.dp)
                                )

                                DropdownMenu(
                                    expanded = bloodDropdownExpanded,
                                    onDismissRequest = { bloodDropdownExpanded = false },
                                    modifier = Modifier.fillMaxWidth(0.85f)
                                ) {
                                    bloodTypesList.forEach { type ->
                                        DropdownMenuItem(
                                            text = { Text(type, fontWeight = FontWeight.Bold) },
                                            onClick = {
                                                bloodType = type
                                                bloodDropdownExpanded = false
                                            }
                                        )
                                    }
                                }
                            }

                            // Allergies
                            OutlinedTextField(
                                value = allergies,
                                onValueChange = { allergies = it },
                                label = { Text("Known Allergies") },
                                leadingIcon = { Icon(Icons.Default.Warning, contentDescription = null, tint = AlertOrange) },
                                placeholder = { Text("e.g. Penicillin, Peanuts, Pollen") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("profile_allergies_field"),
                                shape = RoundedCornerShape(12.dp)
                            )

                            // Medical Conditions
                            OutlinedTextField(
                                value = conditions,
                                onValueChange = { conditions = it },
                                label = { Text("Pre-existing Conditions") },
                                leadingIcon = { Icon(Icons.Default.PersonalInjury, contentDescription = null, tint = ProfPrimary) },
                                placeholder = { Text("e.g. Asthma, Diabetes, Hypertension") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("profile_conditions_field"),
                                shape = RoundedCornerShape(12.dp)
                            )

                            // Active Medications
                            OutlinedTextField(
                                value = medications,
                                onValueChange = { medications = it },
                                label = { Text("Active Medications") },
                                leadingIcon = { Icon(Icons.Default.Medication, contentDescription = null, tint = ProfPrimary) },
                                placeholder = { Text("e.g. Albuterol Inhaler, Insulin") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("profile_medications_field"),
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                    }
                }

                // --- SAVE BUTTON ACTION ---
                item {
                    Button(
                        onClick = {
                            isSaving = true
                            val updatedUser = currentUser.copy(
                                name = name,
                                phone = phone,
                                emergencyContactName = emergencyContactName,
                                emergencyContactPhone = emergencyContactPhone,
                                bloodType = bloodType,
                                allergies = allergies,
                                conditions = conditions,
                                medications = medications,
                                photoUri = photoUriString,
                                medicalInfo = "Blood: $bloodType. Allergies: $allergies. Conditions: $conditions. Meds: $medications"
                            )
                            viewModel.updateUserProfile(updatedUser)
                            isSaving = false
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ProfPrimary,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .shadow(2.dp, RoundedCornerShape(16.dp))
                            .testTag("save_profile_button")
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        } else {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.Save, contentDescription = null)
                                Text("Save Passport Updates", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }

            // --- DIALOG FOR CHOOSE AVATAR PRESET OR DEVICE PHOTO ---
            if (showAvatarDialog) {
                AlertDialog(
                    onDismissRequest = { showAvatarDialog = false },
                    title = {
                        Text(
                            text = "Update Photo",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = ProfOnBackgroundLight
                        )
                    },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            Text(
                                "Select a premium safety avatar symbol or choose a photo from your Android device gallery.",
                                fontSize = 13.sp,
                                color = ProfOnSurfaceVariantLight
                            )

                            // Grid of symbols
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceAround
                            ) {
                                presetAvatars.forEach { (emoji, label) ->
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable {
                                                photoUriString = emoji
                                                showAvatarDialog = false
                                            }
                                            .padding(8.dp)
                                    ) {
                                        Text(emoji, fontSize = 28.sp)
                                        Text(label, fontSize = 10.sp, color = ProfOnSurfaceVariantLight)
                                    }
                                }
                            }

                            HorizontalDivider(color = ProfBorderLight)

                            Button(
                                onClick = {
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                    showAvatarDialog = false
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = ProfPrimaryContainer, contentColor = ProfOnPrimaryContainer)
                            ) {
                                Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Choose from Device Gallery")
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showAvatarDialog = false }) {
                            Text("Cancel")
                        }
                    }
                )
            }
        }
    }
}
