import os

filepath = "app/src/main/java/com/example/ui/screens/HomeScreen.kt"
with open(filepath, "r") as f:
    content = f.read()

target = """            // Big SOS Button
            item {
                Spacer(modifier = Modifier.height(16.dp))
                SosButtonSection(onSosClick = {
                    viewModel.triggerManualSOS()
                    onNavigateToEmergency()
                })
            }"""

replacement = """            // Big SOS Button
            item {
                Spacer(modifier = Modifier.height(16.dp))
                val context = LocalContext.current
                val callPermissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission()
                ) { isGranted: Boolean ->
                    viewModel.triggerManualSOS()
                    onNavigateToEmergency()
                }

                SosButtonSection(onSosClick = {
                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED) {
                        viewModel.triggerManualSOS()
                        onNavigateToEmergency()
                    } else {
                        callPermissionLauncher.launch(Manifest.permission.CALL_PHONE)
                    }
                })
            }"""

if target in content:
    content = content.replace(target, replacement)
    with open(filepath, "w") as f:
        f.write(content)
    print("Fixed HomeScreen SOS button")
else:
    print("Target not found in HomeScreen")

