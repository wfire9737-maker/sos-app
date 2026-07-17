package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.EmergencyContact
import com.example.ui.GuardianViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactsScreen(
    viewModel: GuardianViewModel,
    onNavigateBack: () -> Unit
) {
    val contacts by viewModel.contacts.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    
    // Dialog state
    var showAddEditDialog by remember { mutableStateOf(false) }
    var contactToEdit by remember { mutableStateOf<EmergencyContact?>(null) }
    var contactToDelete by remember { mutableStateOf<EmergencyContact?>(null) }

    // Filter contacts in memory based on search query
    val filteredContacts = remember(contacts, searchQuery) {
        if (searchQuery.isBlank()) {
            contacts
        } else {
            contacts.filter { contact ->
                contact.name.contains(searchQuery, ignoreCase = true) ||
                contact.phone.contains(searchQuery, ignoreCase = true) ||
                contact.relationship.contains(searchQuery, ignoreCase = true) ||
                contact.notes.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Emergency Guardians",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        letterSpacing = (-0.5).sp
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("contacts_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Navigate Back"
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            contactToEdit = null
                            showAddEditDialog = true
                        },
                        modifier = Modifier.testTag("add_contact_top_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add Contact",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    contactToEdit = null
                    showAddEditDialog = true
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                modifier = Modifier
                    .padding(16.dp)
                    .testTag("add_contact_fab")
            ) {
                Icon(
                    imageVector = Icons.Default.PersonAdd,
                    contentDescription = "Add Emergency Contact"
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // --- SEARCH BAR SECTION ---
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search by name, relation, or number...") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search icon",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear search")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .testTag("contacts_search_input")
            )

            // --- SYNC STATUS / SUBHEADER ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (filteredContacts.isEmpty()) "0 contacts" else "${filteredContacts.size} contacts sorted by priority",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF4CAF50))
                    )
                    Text(
                        text = if (viewModel.isDemoMode) "Local Storage Mode" else "Firestore Connected",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF4CAF50)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // --- EMPTY STATE ---
            if (filteredContacts.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = if (searchQuery.isNotEmpty()) "No Contacts Match Search" else "No Emergency Contacts",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (searchQuery.isNotEmpty()) 
                            "Try adjusting keywords or search terms." 
                        else 
                            "Add people (family, doctors, dispatch centers) to receive alert signals automatically.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                }
            } else {
                // --- CONTACTS LIST ---
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .testTag("contacts_lazy_list"),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredContacts, key = { it.id }) { contact ->
                        ContactItemCard(
                            contact = contact,
                            onEdit = {
                                contactToEdit = contact
                                showAddEditDialog = true
                            },
                            onDelete = {
                                contactToDelete = contact
                            }
                        )
                    }
                }
            }
        }

        // --- ADD/EDIT CONTACT DIALOG ---
        if (showAddEditDialog) {
            AddEditContactDialog(
                contact = contactToEdit,
                onDismiss = { showAddEditDialog = false },
                onSave = { updatedContact ->
                    viewModel.saveEmergencyContact(updatedContact)
                    showAddEditDialog = false
                }
            )
        }

        // --- DELETE CONFIRMATION DIALOG ---
        if (contactToDelete != null) {
            AlertDialog(
                onDismissRequest = { contactToDelete = null },
                title = {
                    Text(
                        text = "Remove Emergency Contact?",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                },
                text = {
                    Text(
                        text = "Are you sure you want to remove ${contactToDelete?.name}? They will no longer be contacted automatically when SOS alerts are triggered.",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            contactToDelete?.id?.let { viewModel.deleteEmergencyContact(it) }
                            contactToDelete = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.testTag("confirm_delete_button")
                    ) {
                        Text("Remove")
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
}

@Composable
fun ContactItemCard(
    contact: EmergencyContact,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    // Priority badge configuration
    val (priorityText, priorityBg, priorityFg) = when (contact.priority) {
        1 -> Triple("Primary / High", Color(0xFFFFEBEE), Color(0xFFC62828))
        2 -> Triple("Secondary", Color(0xFFFFF3E0), Color(0xFFE65100))
        else -> Triple("Tertiary / Backup", Color(0xFFECEFF1), Color(0xFF37474F))
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xF1E2E5EC), RoundedCornerShape(20.dp))
            .testTag("contact_card_${contact.id}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Emoji avatar sphere
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = contact.avatarEmoji,
                        fontSize = 24.sp
                    )
                }

                // Core Name, Relationship, and Phone Info
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = contact.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onBackground,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.testTag("contact_name_${contact.id}")
                        )
                        
                        // Relationship Tag
                        if (contact.relationship.isNotBlank()) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(MaterialTheme.colorScheme.secondaryContainer)
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = contact.relationship,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = contact.phone,
                        fontWeight = FontWeight.Medium,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.testTag("contact_phone_${contact.id}")
                    )
                }

                // Quick Call Option Icon
                IconButton(
                    onClick = onEdit,
                    modifier = Modifier.testTag("edit_contact_btn_${contact.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Contact",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.testTag("delete_contact_btn_${contact.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Delete Contact",
                        tint = Color(0xFFC62828),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Expanded notes and priority block
            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = Color(0xFFF1F3F7))
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Priority Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(priorityBg)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PriorityHigh,
                            contentDescription = null,
                            tint = priorityFg,
                            modifier = Modifier.size(10.dp)
                        )
                        Text(
                            text = priorityText,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = priorityFg
                        )
                    }
                }

                if (contact.notes.isNotBlank()) {
                    Text(
                        text = contact.notes,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.End,
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 16.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditContactDialog(
    contact: EmergencyContact?,
    onDismiss: () -> Unit,
    onSave: (EmergencyContact) -> Unit
) {
    var name by remember { mutableStateOf(contact?.name ?: "") }
    var phone by remember { mutableStateOf(contact?.phone ?: "") }
    var relationship by remember { mutableStateOf(contact?.relationship ?: "") }
    var priority by remember { mutableStateOf(contact?.priority ?: 1) }
    var notes by remember { mutableStateOf(contact?.notes ?: "") }
    var avatarEmoji by remember { mutableStateOf(contact?.avatarEmoji ?: "👤") }

    val presetEmojis = listOf("🚨", "🛡️", "🩺", "🏡", "🌲", "👵", "🧓", "📞", "🏥", "👤")

    var nameError by remember { mutableStateOf(false) }
    var phoneError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (contact == null) "New Emergency Contact" else "Edit Emergency Contact",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // --- AVATAR PICKER ROW ---
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Choose Visual Emblem",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Current Active Emoji
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(avatarEmoji, fontSize = 22.sp)
                            }

                            // Picker row
                            Row(
                                modifier = Modifier.weight(1f).padding(start = 12.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                presetEmojis.take(6).forEach { emoji ->
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (avatarEmoji == emoji) 
                                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                                else 
                                                    Color(0xFFF1F3F7)
                                            )
                                            .border(
                                                BorderStroke(
                                                    if (avatarEmoji == emoji) 2.dp else 0.dp,
                                                    MaterialTheme.colorScheme.primary
                                                ),
                                                CircleShape
                                            )
                                            .clickable { avatarEmoji = emoji },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(emoji, fontSize = 16.sp)
                                    }
                                }
                            }
                        }
                    }
                }

                // --- CONTACT FORM FIELDS ---
                item {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { 
                            name = it
                            nameError = false
                        },
                        label = { Text("Full Name *") },
                        isError = nameError,
                        supportingText = {
                            if (nameError) {
                                Text("Name is required", color = MaterialTheme.colorScheme.error)
                            }
                        },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("dialog_contact_name"),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                item {
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { 
                            phone = it
                            phoneError = false
                        },
                        label = { Text("Contact Number *") },
                        isError = phoneError,
                        supportingText = {
                            if (phoneError) {
                                Text("Number is required", color = MaterialTheme.colorScheme.error)
                            }
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("dialog_contact_phone"),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                item {
                    OutlinedTextField(
                        value = relationship,
                        onValueChange = { relationship = it },
                        label = { Text("Relationship (e.g. Doctor, Spouse)") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("dialog_contact_relationship"),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                // --- PRIORITY SELECTOR SEGMENTS ---
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Response Dispatch Priority Level",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(1, 2, 3).forEach { level ->
                                val (title, selectedColor, unselectedColor) = when (level) {
                                    1 -> Triple("P1 - High", Color(0xFFC62828), Color(0xFFFFEBEE))
                                    2 -> Triple("P2 - Med", Color(0xFFE65100), Color(0xFFFFF3E0))
                                    else -> Triple("P3 - Low", Color(0xFF37474F), Color(0xFFECEFF1))
                                }

                                Button(
                                    onClick = { priority = level },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (priority == level) selectedColor else unselectedColor,
                                        contentColor = if (priority == level) Color.White else selectedColor
                                    ),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(40.dp)
                                        .testTag("priority_level_btn_$level"),
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Text(title, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                item {
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Safety Notes & Key Access Codes") },
                        placeholder = { Text("e.g. Has spare key, medical proxy...") },
                        maxLines = 3,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("dialog_contact_notes"),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isBlank()) {
                        nameError = true
                    }
                    if (phone.isBlank()) {
                        phoneError = true
                    }
                    if (name.isNotBlank() && phone.isNotBlank()) {
                        onSave(
                            EmergencyContact(
                                id = contact?.id ?: "",
                                userId = contact?.userId ?: "demo-uid-123",
                                name = name.trim(),
                                phone = phone.trim(),
                                relationship = relationship.trim(),
                                priority = priority,
                                notes = notes.trim(),
                                avatarEmoji = avatarEmoji
                            )
                        )
                    }
                },
                modifier = Modifier.testTag("dialog_save_contact_button")
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
}
