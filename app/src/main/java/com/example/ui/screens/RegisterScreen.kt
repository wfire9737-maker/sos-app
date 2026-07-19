package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.service.AuthState
import com.example.ui.GuardianViewModel
import com.example.ui.theme.EmergencyRed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    viewModel: GuardianViewModel,
    onNavigateToLogin: () -> Unit
) {
    val authState by viewModel.authState.collectAsState()

    // Form inputs
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var medicalInfo by remember { mutableStateOf("") }
    var emergencyContactName by remember { mutableStateOf("") }
    var emergencyContactPhone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    var passwordVisible by remember { mutableStateOf(false) }
    var validationError by remember { mutableStateOf<String?>(null) }

    val focusManager = LocalFocusManager.current
    val scrollState = rememberScrollState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.surfaceVariant
                    )
                )
            )
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Header Back Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onNavigateToLogin) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Navigate Back",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Create Safety Profile",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // SECTION 1: Personal Account Info Card
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f),
                        RoundedCornerShape(24.dp)
                    )
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "1. Personal Information",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = EmergencyRed
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // Full Name
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Full Name") },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = EmergencyRed, focusedLabelColor = EmergencyRed),
                        modifier = Modifier.fillMaxWidth().testTag("reg_name")
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Email Address
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email Address") },
                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = EmergencyRed, focusedLabelColor = EmergencyRed),
                        modifier = Modifier.fillMaxWidth().testTag("reg_email")
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Phone Number
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("Personal Mobile Number") },
                        leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = EmergencyRed, focusedLabelColor = EmergencyRed),
                        modifier = Modifier.fillMaxWidth().testTag("reg_phone")
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // SECTION 2: Medical & Emergency Contact Card
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f),
                        RoundedCornerShape(24.dp)
                    )
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "2. Critical Rescue Vitals",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = EmergencyRed
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // Medical Details
                    OutlinedTextField(
                        value = medicalInfo,
                        onValueChange = { medicalInfo = it },
                        label = { Text("Medical Notes (Blood type, Allergies)") },
                        leadingIcon = { Icon(Icons.Default.MedicalServices, contentDescription = null) },
                        placeholder = { Text("e.g. Blood type O+, No drug allergies") },
                        singleLine = false,
                        maxLines = 3,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = EmergencyRed, focusedLabelColor = EmergencyRed),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Emergency Contact Name
                    OutlinedTextField(
                        value = emergencyContactName,
                        onValueChange = { emergencyContactName = it },
                        label = { Text("Guardian Contact Name") },
                        leadingIcon = { Icon(Icons.Default.ContactPhone, contentDescription = null) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = EmergencyRed, focusedLabelColor = EmergencyRed),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Emergency Contact Phone
                    OutlinedTextField(
                        value = emergencyContactPhone,
                        onValueChange = { emergencyContactPhone = it },
                        label = { Text("Guardian Contact Phone") },
                        leadingIcon = { Icon(Icons.Default.Emergency, contentDescription = null) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = EmergencyRed, focusedLabelColor = EmergencyRed),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // SECTION 3: Credentials Card
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f),
                        RoundedCornerShape(24.dp)
                    )
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "3. Security Configuration",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = EmergencyRed
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // Password
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password") },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = null
                                )
                            }
                        },
                        singleLine = true,
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = EmergencyRed, focusedLabelColor = EmergencyRed),
                        modifier = Modifier.fillMaxWidth().testTag("reg_pass")
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Confirm Password
                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it },
                        label = { Text("Confirm Password") },
                        leadingIcon = { Icon(Icons.Default.LockReset, contentDescription = null) },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = EmergencyRed, focusedLabelColor = EmergencyRed),
                        modifier = Modifier.fillMaxWidth().testTag("reg_confirm_pass")
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Error display
            val finalError = validationError ?: (authState as? AuthState.Error)?.message
            AnimatedVisibility(
                visible = finalError != null,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                if (finalError != null) {
                    Text(
                        text = finalError,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action Button or Loader
            if (authState is AuthState.Loading) {
                Box(
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = EmergencyRed, modifier = Modifier.size(28.dp))
                }
            } else {
                Button(
                    onClick = {
                        focusManager.clearFocus()
                        // Validation checks
                        if (name.isBlank() || email.isBlank() || phone.isBlank() || password.isBlank()) {
                            validationError = "Please fill in all mandatory account fields."
                        } else if (password != confirmPassword) {
                            validationError = "Passwords do not match."
                        } else if (password.length < 6) {
                            validationError = "Password must be at least 6 characters long."
                        } else {
                            validationError = null
                            viewModel.registerUser(
                                name = name.trim(),
                                email = email.trim(),
                                phone = phone.trim(),
                                medical = medicalInfo.trim(),
                                contactName = emergencyContactName.trim(),
                                contactPhone = emergencyContactPhone.trim(),
                                pass = password
                            )
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmergencyRed, contentColor = Color.White),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("register_submit_button")
                ) {
                    Text("REGISTER & SECURE PROFILE", fontSize = 15.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Footer
            Row(
                modifier = Modifier.padding(bottom = 32.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Already have an account? ", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    text = "Sign In",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = EmergencyRed,
                    modifier = Modifier
                        .clickable { onNavigateToLogin() }
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                )
            }
        }
    }
}
