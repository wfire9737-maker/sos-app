package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import com.example.utils.hasCallPhonePermission
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import com.example.model.EmergencyContact
import com.example.ui.GuardianViewModel

private fun extractContactDetails(context: android.content.Context, contactUri: Uri): Pair<String?, List<String>> {
    var displayName: String? = null
    val phoneNumbers = mutableListOf<String>()
    var contactId: String? = null

    try {
        // Query contact details (Display name & contact ID)
        context.contentResolver.query(
            contactUri,
            arrayOf(
                ContactsContract.Contacts._ID,
                ContactsContract.Contacts.DISPLAY_NAME
            ),
            null,
            null,
            null
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                val idIndex = cursor.getColumnIndex(ContactsContract.Contacts._ID)
                val nameIndex = cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)
                if (idIndex != -1) {
                    contactId = cursor.getString(idIndex)
                }
                if (nameIndex != -1) {
                    displayName = cursor.getString(nameIndex)
                }
            }
        }

        // If contact ID is found, query phone numbers associated with this contact
        if (contactId != null) {
            context.contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER),
                "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ?",
                arrayOf(contactId),
                null
            )?.use { phoneCursor ->
                val numberIndex = phoneCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                while (phoneCursor.moveToNext()) {
                    if (numberIndex != -1) {
                        val num = phoneCursor.getString(numberIndex)
                        if (!num.isNullOrBlank()) {
                            phoneNumbers.add(num.trim())
                        }
                    }
                }
            }
        }

        // Fallback: Query directly from URI in case it is already a Phone data URI
        if (phoneNumbers.isEmpty()) {
            context.contentResolver.query(
                contactUri,
                arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER),
                null,
                null,
                null
            )?.use { directCursor ->
                val numIdx = directCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                while (directCursor.moveToNext()) {
                    if (numIdx != -1) {
                        val num = directCursor.getString(numIdx)
                        if (!num.isNullOrBlank()) {
                            phoneNumbers.add(num.trim())
                        }
                    }
                }
            }
        }
    } catch (e: Exception) {
        android.util.Log.e("ContactsScreen", "Error extracting contact details", e)
    }

    return Pair(displayName, phoneNumbers.distinct())
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactsScreen(
    viewModel: GuardianViewModel,
    onNavigateBack: () -> Unit
) {
    val contacts by viewModel.contacts.collectAsState()
    
    var showAddDialog by remember { mutableStateOf(false) }
    var contactToEdit by remember { mutableStateOf<EmergencyContact?>(null) }
    var contactToDelete by remember { mutableStateOf<EmergencyContact?>(null) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Emergency Contacts", fontWeight = FontWeight.Bold) },
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
        floatingActionButton = {
            if (contacts.size < 5) {
                ExtendedFloatingActionButton(
                    onClick = { showAddDialog = true },
                    icon = { Icon(Icons.Default.Add, contentDescription = "Add Contact") },
                    text = { Text("Add Contact") },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            Text(
                "You can add up to 5 trusted people who will be notified during an emergency.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 16.dp)
            )

            if (contacts.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.GroupAdd,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "No contacts added yet",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 80.dp) // space for FAB
                ) {
                    items(contacts, key = { it.id }) { contact ->
                        ContactCard(
                            contact = contact,
                            onEdit = { contactToEdit = contact },
                            onDelete = { contactToDelete = contact }
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog || contactToEdit != null) {
        val initialContact = contactToEdit
        AddEditContactDialog(
            contact = initialContact,
            onDismiss = { 
                showAddDialog = false
                contactToEdit = null
            },
            onSave = { updatedContact ->
                viewModel.saveEmergencyContact(updatedContact)
                showAddDialog = false
                contactToEdit = null
            }
        )
    }

    contactToDelete?.let { contact ->
        AlertDialog(
            onDismissRequest = { contactToDelete = null },
            title = { Text("Delete Contact") },
            text = { Text("Are you sure you want to remove ${contact.name} from your emergency contacts?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteEmergencyContact(contact.id)
                        contactToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { contactToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun ContactCard(
    contact: EmergencyContact,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    val callPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            val intent = Intent(Intent.ACTION_CALL).apply { data = Uri.parse("tel:${contact.phone}") }
            context.startActivity(intent)
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = contact.name.take(1).uppercase(),
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            
            // Info
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = contact.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (contact.priority == 1) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = "PRIMARY",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                Text(
                    text = contact.phone,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 14.sp
                )
                if (contact.relationship.isNotBlank()) {
                    Text(
                        text = contact.relationship,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                if (!contact.customSmsTemplate.isNullOrBlank()) {
                    Text(
                        text = "💬 \"${contact.customSmsTemplate}\"",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            
            // Actions
            Row {
                IconButton(onClick = {
                    if (context.hasCallPhonePermission()) {
                        val intent = Intent(Intent.ACTION_CALL).apply { data = Uri.parse("tel:${contact.phone}") }
                        context.startActivity(intent)
                    } else {
                        callPermissionLauncher.launch(Manifest.permission.CALL_PHONE)
                    }
                }) {
                    Icon(Icons.Default.Phone, contentDescription = "Call", tint = MaterialTheme.colorScheme.primary)
                }
                IconButton(onClick = {
                    val intent = Intent(Intent.ACTION_SENDTO).apply {
                        data = Uri.parse("smsto:${contact.phone}")
                    }
                    try {
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        // Handle exception if SMS app is not found
                    }
                }) {
                    Icon(Icons.Default.Sms, contentDescription = "SMS", tint = MaterialTheme.colorScheme.primary)
                }
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
fun AddEditContactDialog(
    contact: EmergencyContact?,
    onDismiss: () -> Unit,
    onSave: (EmergencyContact) -> Unit
) {
    val context = LocalContext.current
    var name by remember { mutableStateOf(contact?.name ?: "") }
    var phone by remember { mutableStateOf(contact?.phone ?: "") }
    var relationship by remember { mutableStateOf(contact?.relationship ?: "") }
    var isPrimary by remember { mutableStateOf(contact?.priority == 1) }
    var customSmsTemplate by remember { mutableStateOf(contact?.customSmsTemplate) }
    var showTemplateEditorDialog by remember { mutableStateOf(false) }
    var tempTemplateText by remember { mutableStateOf("") }

    var showPhoneSelectionDialog by remember { mutableStateOf(false) }
    var pendingContactName by remember { mutableStateOf("") }
    var pendingPhoneNumbers by remember { mutableStateOf<List<String>>(emptyList()) }

    val contactPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickContact()
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult

        val (pickedName, pickedNumbers) = extractContactDetails(context, uri)
        if (pickedNumbers.isEmpty()) {
            if (!pickedName.isNullOrBlank()) {
                name = pickedName
            }
            Toast.makeText(context, "This contact does not have a phone number.", Toast.LENGTH_LONG).show()
        } else if (pickedNumbers.size == 1) {
            if (!pickedName.isNullOrBlank()) {
                name = pickedName
            }
            phone = pickedNumbers[0]
        } else {
            pendingContactName = pickedName ?: name
            pendingPhoneNumbers = pickedNumbers
            showPhoneSelectionDialog = true
        }
    }
    
    val isEdit = contact != null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isEdit) "Edit Contact" else "Add Contact", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // Pick from Phone Contacts Button
                OutlinedButton(
                    onClick = {
                        try {
                            contactPickerLauncher.launch(null)
                        } catch (e: Exception) {
                            Toast.makeText(context, "Unable to open phone contacts: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("pick_from_phone_contacts_btn"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Contacts, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Pick from Phone Contacts")
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                val isPhoneValid = phone.isEmpty() || phone.matches(Regex("^[+]?[0-9\\s-]{7,15}$"))
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Phone Number") },
                    leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    isError = !isPhoneValid,
                    supportingText = if (!isPhoneValid) { { Text("Invalid phone number format") } } else null,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = relationship,
                    onValueChange = { relationship = it },
                    label = { Text("Relationship (e.g. Spouse)") },
                    leadingIcon = { Icon(Icons.Default.Favorite, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Switch(
                        checked = isPrimary,
                        onCheckedChange = { isPrimary = it }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Set as Primary Contact")
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "SMS Template",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (customSmsTemplate.isNullOrBlank()) {
                                    "Default emergency message"
                                } else {
                                    customSmsTemplate!!
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = if (customSmsTemplate.isNullOrBlank()) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        TextButton(
                            onClick = {
                                tempTemplateText = customSmsTemplate ?: ""
                                showTemplateEditorDialog = true
                            }
                        ) {
                            Text(if (customSmsTemplate.isNullOrBlank()) "Set Template" else "Edit Message")
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank() && phone.isNotBlank()) {
                        onSave(
                            contact?.copy(
                                name = name.trim(),
                                phone = phone.trim(),
                                relationship = relationship.trim(),
                                priority = if (isPrimary) 1 else 2,
                                customSmsTemplate = customSmsTemplate?.trim()?.ifEmpty { null }
                            ) ?: EmergencyContact(
                                id = "contact-${System.currentTimeMillis()}",
                                userId = "", // Will be set by viewModel
                                name = name.trim(),
                                phone = phone.trim(),
                                relationship = relationship.trim(),
                                priority = if (isPrimary) 1 else 2,
                                customSmsTemplate = customSmsTemplate?.trim()?.ifEmpty { null }
                            )
                        )
                    }
                },
                enabled = name.isNotBlank() && phone.isNotBlank() && phone.matches(Regex("^[+]?[0-9\\s-]{7,15}$"))
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )

    if (showTemplateEditorDialog) {
        AlertDialog(
            onDismissRequest = { showTemplateEditorDialog = false },
            title = { Text("SMS Template", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Customize the message sent to this contact. Emergency event, location, and time will be automatically included.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = tempTemplateText,
                        onValueChange = { tempTemplateText = it },
                        label = { Text("Custom message") },
                        placeholder = { Text("e.g. Please help me immediately.") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 100.dp),
                        maxLines = 4
                    )
                    if (tempTemplateText.isNotBlank()) {
                        TextButton(
                            onClick = { tempTemplateText = "" },
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text("Use Default Message", fontSize = 12.sp)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val trimmed = tempTemplateText.trim()
                        customSmsTemplate = trimmed.ifEmpty { null }
                        showTemplateEditorDialog = false
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showTemplateEditorDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showPhoneSelectionDialog && pendingPhoneNumbers.isNotEmpty()) {
        AlertDialog(
            onDismissRequest = { 
                showPhoneSelectionDialog = false
                pendingPhoneNumbers = emptyList()
            },
            title = { Text("Select Phone Number", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Choose which phone number to use for ${pendingContactName.ifBlank { "this contact" }}:",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    pendingPhoneNumbers.forEach { numberOption ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    if (pendingContactName.isNotBlank()) {
                                        name = pendingContactName
                                    }
                                    phone = numberOption
                                    showPhoneSelectionDialog = false
                                    pendingPhoneNumbers = emptyList()
                                },
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Phone,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = numberOption,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(
                    onClick = { 
                        showPhoneSelectionDialog = false
                        pendingPhoneNumbers = emptyList()
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}
