import os

filepath = "app/src/main/java/com/example/ui/screens/HomeScreen.kt"
with open(filepath, "r") as f:
    content = f.read()

# Replace imports if needed
if "import com.example.ui.rememberLocationPermissionHandler" not in content:
    content = content.replace("import androidx.compose.ui.Modifier", "import androidx.compose.ui.Modifier\nimport com.example.ui.rememberLocationPermissionHandler")

target1 = """    val callPermissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        viewModel.triggerManualSOS()
        onNavigateToEmergency()
    }"""
    
replacement1 = """    val sosTriggerHandler = rememberLocationPermissionHandler {
        viewModel.triggerManualSOS()
        onNavigateToEmergency()
    }"""
    
content = content.replace(target1, replacement1)

target2 = """            // Big SOS Button
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
            
replacement2 = """            // Big SOS Button
            item {
                Spacer(modifier = Modifier.height(16.dp))
                SosButtonSection(onSosClick = {
                    sosTriggerHandler()
                })
            }"""

content = content.replace(target2, replacement2)

with open(filepath, "w") as f:
    f.write(content)
print("Updated HomeScreen permissions handling")
