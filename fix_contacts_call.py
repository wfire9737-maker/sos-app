import os

filepath = "app/src/main/java/com/example/ui/screens/ContactsScreen.kt"
with open(filepath, "r") as f:
    content = f.read()

target = """    val callPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            val intent = Intent(Intent.ACTION_CALL).apply { data = Uri.parse("tel:${contact.phone}") }
            context.startActivity(intent)
        } else {
            val intent = Intent(Intent.ACTION_DIAL).apply { data = Uri.parse("tel:${contact.phone}") }
            context.startActivity(intent)
        }
    }"""

replacement = """    val callPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            val intent = Intent(Intent.ACTION_CALL).apply { data = Uri.parse("tel:${contact.phone}") }
            context.startActivity(intent)
        }
    }"""

if target in content:
    content = content.replace(target, replacement)
    with open(filepath, "w") as f:
        f.write(content)
    print("Fixed ContactsScreen call logic")
else:
    print("Target not found in ContactsScreen")

