import os

filepath = "app/src/main/java/com/example/ui/screens/HomeScreen.kt"
with open(filepath, "r") as f:
    content = f.read()

target1 = """    val authState by viewModel.authState.collectAsState()"""
replacement1 = """    val context = androidx.compose.ui.platform.LocalContext.current
    val callPermissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        viewModel.triggerManualSOS()
        onNavigateToEmergency()
    }
    
    val authState by viewModel.authState.collectAsState()"""

target2 = """            // Big SOS Button
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
replacement2 = """            // Big SOS Button
            item {
                Spacer(modifier = Modifier.height(16.dp))
                SosButtonSection(onSosClick = {
                    if (androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.CALL_PHONE) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                        viewModel.triggerManualSOS()
                        onNavigateToEmergency()
                    } else {
                        callPermissionLauncher.launch(android.Manifest.permission.CALL_PHONE)
                    }
                })
            }"""

if target1 in content and target2 in content:
    content = content.replace(target1, replacement1)
    content = content.replace(target2, replacement2)
    with open(filepath, "w") as f:
        f.write(content)
    print("Fixed HomeScreen correctly")
else:
    print("Target not found")
    if target1 not in content: print("t1 missing")
    if target2 not in content: print("t2 missing")
