import os

filepath = "app/src/main/java/com/example/ui/screens/ContactsScreen.kt"
with open(filepath, "r") as f:
    content = f.read()

target = """fun AddEditContactDialog(
    contact: EmergencyContact?,
    onDismiss: () -> Unit,
    onSave: (EmergencyContact) -> Unit
) {
    var name by remember { mutableStateOf(contact?.name ?: "") }
    var phone by remember { mutableStateOf(contact?.phone ?: "") }
    var relationship by remember { mutableStateOf(contact?.relationship ?: "") }
    
    val isEdit = contact != null"""

replacement = """fun AddEditContactDialog(
    contact: EmergencyContact?,
    onDismiss: () -> Unit,
    onSave: (EmergencyContact) -> Unit
) {
    var name by remember { mutableStateOf(contact?.name ?: "") }
    var phone by remember { mutableStateOf(contact?.phone ?: "") }
    var relationship by remember { mutableStateOf(contact?.relationship ?: "") }
    var isPrimary by remember { mutableStateOf(contact?.priority == 1) }
    
    val isEdit = contact != null"""

if target in content:
    content = content.replace(target, replacement)
    
    target2 = """                OutlinedTextField(
                    value = relationship,
                    onValueChange = { relationship = it },
                    label = { Text("Relationship (e.g. Spouse)") },
                    leadingIcon = { Icon(Icons.Default.Favorite, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },"""
        
    replacement2 = """                OutlinedTextField(
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
            }
        },"""
        
    content = content.replace(target2, replacement2)
    
    target3 = """                            contact?.copy(
                                name = name.trim(),
                                phone = phone.trim(),
                                relationship = relationship.trim()
                            ) ?: EmergencyContact(
                                id = "contact-${System.currentTimeMillis()}",
                                userId = "", // Will be set by viewModel
                                name = name.trim(),
                                phone = phone.trim(),
                                relationship = relationship.trim()
                            )"""
                            
    replacement3 = """                            contact?.copy(
                                name = name.trim(),
                                phone = phone.trim(),
                                relationship = relationship.trim(),
                                priority = if (isPrimary) 1 else 2
                            ) ?: EmergencyContact(
                                id = "contact-${System.currentTimeMillis()}",
                                userId = "", // Will be set by viewModel
                                name = name.trim(),
                                phone = phone.trim(),
                                relationship = relationship.trim(),
                                priority = if (isPrimary) 1 else 2
                            )"""
                            
    content = content.replace(target3, replacement3)
    
    target4 = """            // Info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = contact.name,"""
                    
    replacement4 = """            // Info
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
                // Text for Name was replaced by the Row above. Now we need to remove the original Text
                // But wait, my target4 only captured part of it, let me adjust it."""

    # Instead of target4, let's use sed in bash to replace the exact block
    
    with open(filepath, "w") as f:
        f.write(content)
    print("Fixed ContactsScreen primary logic again!")
else:
    print("Target not found again!")
