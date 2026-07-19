package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
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
    val currentUser = (authState as? AuthState.Success)?.user ?: User(name = "User")

    var name by remember { mutableStateOf(currentUser.name) }
    var email by remember { mutableStateOf(currentUser.email) }
    var phone by remember { mutableStateOf(currentUser.phone) }
    var bloodGroup by remember { mutableStateOf(currentUser.bloodType) }
    var medicalConditions by remember { mutableStateOf(currentUser.conditions) }
    var emergencyNotes by remember { mutableStateOf(currentUser.medicalInfo) }
    var profilePhotoUri by remember { mutableStateOf<Uri?>(if (!currentUser.photoUri.isNullOrBlank()) Uri.parse(currentUser.photoUri ?: "") else null) }
    
    var isEditing by remember { mutableStateOf(false) }
    var showSaveDialog by remember { mutableStateOf(false) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri -> uri?.let { profilePhotoUri = it } }
    )

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Medical Profile", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (isEditing) {
                        TextButton(onClick = { showSaveDialog = true }) {
                            Text("Save", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                    } else {
                        IconButton(onClick = { isEditing = true }) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit Profile")
                        }
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
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                            .clip(CircleShape)
                            .clickable(enabled = isEditing) {
                                photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (profilePhotoUri != null) {
                            AsyncImage(
                                model = profilePhotoUri,
                                contentDescription = "Profile Photo",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        
                        if (isEditing) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = 0.3f))
                            )
                            Icon(Icons.Default.CameraAlt, contentDescription = null, tint = Color.White)
                        }
                    }
                }
            }

            item {
                Text("Personal Information", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        ProfileTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = "Full Name",
                            icon = Icons.Default.Person,
                            readOnly = !isEditing
                        )
                        ProfileTextField(
                            value = email,
                            onValueChange = { email = it },
                            label = "Email Address",
                            icon = Icons.Default.Email,
                            readOnly = !isEditing,
                            keyboardType = KeyboardType.Email
                        )
                        ProfileTextField(
                            value = phone,
                            onValueChange = { phone = it },
                            label = "Mobile Number",
                            icon = Icons.Default.Phone,
                            readOnly = !isEditing,
                            keyboardType = KeyboardType.Phone
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text("Emergency & Medical", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)),
                    shape = RoundedCornerShape(16.dp),
                    border = borderStrokeIf(isEditing, MaterialTheme.colorScheme.error.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        ProfileTextField(
                            value = bloodGroup,
                            onValueChange = { bloodGroup = it },
                            label = "Blood Group",
                            icon = Icons.Default.WaterDrop,
                            readOnly = !isEditing,
                            isErrorColors = true
                        )
                        ProfileTextField(
                            value = medicalConditions,
                            onValueChange = { medicalConditions = it },
                            label = "Medical Conditions",
                            icon = Icons.Default.LocalHospital,
                            readOnly = !isEditing,
                            isErrorColors = true,
                            singleLine = false
                        )
                        ProfileTextField(
                            value = emergencyNotes,
                            onValueChange = { emergencyNotes = it },
                            label = "Emergency Notes",
                            icon = Icons.Default.Warning,
                            readOnly = !isEditing,
                            isErrorColors = true,
                            singleLine = false
                        )
                    }
                }
            }
            
            item { Spacer(modifier = Modifier.height(32.dp)) }
        }
    }

    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            title = { Text("Save Profile", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to save these changes?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateUserProfile(
                            currentUser.copy(
                                name = name,
                                email = email,
                                phone = phone,
                                bloodType = bloodGroup,
                                conditions = medicalConditions,
                                medicalInfo = emergencyNotes,
                                photoUri = profilePhotoUri?.toString()
                            )
                        )
                        isEditing = false
                        showSaveDialog = false
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSaveDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun ProfileTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    readOnly: Boolean,
    keyboardType: KeyboardType = KeyboardType.Text,
    isErrorColors: Boolean = false,
    singleLine: Boolean = true
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        leadingIcon = { 
            Icon(
                icon, 
                contentDescription = null, 
                tint = if (isErrorColors) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
            ) 
        },
        readOnly = readOnly,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        singleLine = singleLine,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = if (readOnly) {
            OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = Color.Transparent,
                focusedBorderColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                focusedContainerColor = Color.Transparent
            )
        } else if (isErrorColors) {
            OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.error,
                focusedLabelColor = MaterialTheme.colorScheme.error
            )
        } else {
            OutlinedTextFieldDefaults.colors()
        }
    )
}

@Composable
fun borderStrokeIf(condition: Boolean, color: Color) = 
    if (condition) androidx.compose.foundation.BorderStroke(1.dp, color) else null

