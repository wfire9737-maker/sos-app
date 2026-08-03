import os

filepath = "app/src/main/java/com/example/ui/screens/EmergencyScreen.kt"
with open(filepath, "r") as f:
    content = f.read()

target = """    val callPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            val intent = Intent(Intent.ACTION_CALL).apply { data = Uri.parse("tel:$primaryContactPhone") }
            context.startActivity(intent)
        } else {
            // Fallback to dial
            val intent = Intent(Intent.ACTION_DIAL).apply { data = Uri.parse("tel:$primaryContactPhone") }
            context.startActivity(intent)
        }
    }"""

replacement = """    val callPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        val isEmergencyNumber = primaryContactPhone == "911" || primaryContactPhone == "112" || primaryContactPhone == "999"
        if (isGranted && !isEmergencyNumber) {
            try {
                val intent = Intent(Intent.ACTION_CALL).apply { data = Uri.parse("tel:$primaryContactPhone") }
                context.startActivity(intent)
            } catch (e: Exception) {
                val intent = Intent(Intent.ACTION_DIAL).apply { data = Uri.parse("tel:$primaryContactPhone") }
                context.startActivity(intent)
            }
        } else {
            // Fallback to dial
            val intent = Intent(Intent.ACTION_DIAL).apply { data = Uri.parse("tel:$primaryContactPhone") }
            context.startActivity(intent)
        }
    }"""

if target in content:
    content = content.replace(target, replacement)
    with open(filepath, "w") as f:
        f.write(content)
    print("Fixed EmergencyScreen launcher")
else:
    print("Target not found in EmergencyScreen launcher")
