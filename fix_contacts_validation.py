import os

filepath = "app/src/main/java/com/example/ui/screens/ContactsScreen.kt"
with open(filepath, "r") as f:
    content = f.read()
    
target = """                enabled = name.isNotBlank() && phone.isNotBlank()"""
replacement = """                enabled = name.isNotBlank() && phone.isNotBlank() && phone.matches(Regex("^[+]?[0-9\\\\s-]{7,15}$"))"""

if target in content:
    content = content.replace(target, replacement)
    
    # Let's also show an error on the phone text field if it's invalid
    target2 = """                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Phone Number") },
                    leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )"""
                
    replacement2 = """                val isPhoneValid = phone.isEmpty() || phone.matches(Regex("^[+]?[0-9\\\\s-]{7,15}$"))
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
                )"""
    content = content.replace(target2, replacement2)
    
    with open(filepath, "w") as f:
        f.write(content)
    print("Fixed ContactsScreen validation")
else:
    print("Target not found")
