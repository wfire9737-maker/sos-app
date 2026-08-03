import os

filepath = "app/src/main/java/com/example/ui/screens/ContactsScreen.kt"
with open(filepath, "r") as f:
    content = f.read()

target = """    var phone by remember { mutableStateOf(contact?.phone ?: "") }
    var relationship by remember { mutableStateOf(contact?.relationship ?: "") }

    AlertDialog("""

replacement = """    var phone by remember { mutableStateOf(contact?.phone ?: "") }
    var relationship by remember { mutableStateOf(contact?.relationship ?: "") }
    var isPrimary by remember { mutableStateOf(contact?.priority == 1) }

    AlertDialog("""

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
                    Text("Set as Primary Contact", style = MaterialTheme.typography.bodyMedium)
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
    
    # Also add "Primary" badge on the UI
    target4 = """            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = contact.name,"""
    replacement4 = """            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = contact.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
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
                    text = contact.relationship,"""
                    
    content = content.replace(target4, replacement4)

    with open(filepath, "w") as f:
        f.write(content)
    print("Fixed ContactsScreen primary logic")
else:
    print("Target not found")
