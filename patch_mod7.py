import re

with open("app/src/main/java/com/example/ui/screens/DeveloperDashboardScreen.kt", "r") as f:
    content = f.read()

imports = """
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.content.Intent
import android.net.Uri
import android.provider.Settings
"""
if "import androidx.activity.compose.rememberLauncherForActivityResult" not in content:
    content = content.replace("import androidx.compose.ui.Modifier", imports + "import androidx.compose.ui.Modifier")

module7_code = """
            Spacer(modifier = Modifier.height(24.dp))
            Text("Module 7 - Permission Testing", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

            val permissionsState by viewModel.permissionsState.collectAsState()
            val isBluetoothPermissionGranted = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
            } else {
                ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH) == PackageManager.PERMISSION_GRANTED
            }

            StatusItemCustomColor("Location Permission", if(permissionsState.locationGranted) "Granted" else "Denied", if(permissionsState.locationGranted) Color.Green else Color.Red)
            StatusItemCustomColor("Bluetooth Permission", if(isBluetoothPermissionGranted) "Granted" else "Denied", if(isBluetoothPermissionGranted) Color.Green else Color.Red)
            StatusItemCustomColor("SMS Permission", if(permissionsState.smsGranted) "Granted" else "Denied", if(permissionsState.smsGranted) Color.Green else Color.Red)
            StatusItemCustomColor("Call Permission", if(permissionsState.callsGranted) "Granted" else "Denied", if(permissionsState.callsGranted) Color.Green else Color.Red)
            StatusItemCustomColor("Notification Permission", if(permissionsState.notificationsGranted) "Granted" else "Denied", if(permissionsState.notificationsGranted) Color.Green else Color.Red)

            val multiplePermissionsLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestMultiplePermissions()
            ) {
                viewModel.refreshPermissions(context)
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { 
                    viewModel.refreshPermissions(context)
                }, modifier = Modifier.weight(1f)) {
                    Text("Check Permissions")
                }
                
                Button(onClick = { 
                    val permsToRequest = mutableListOf<String>()
                    if (!permissionsState.locationGranted) permsToRequest.add(Manifest.permission.ACCESS_FINE_LOCATION)
                    if (!isBluetoothPermissionGranted) {
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                            permsToRequest.add(Manifest.permission.BLUETOOTH_CONNECT)
                            permsToRequest.add(Manifest.permission.BLUETOOTH_SCAN)
                        } else {
                            permsToRequest.add(Manifest.permission.BLUETOOTH)
                        }
                    }
                    if (!permissionsState.smsGranted) permsToRequest.add(Manifest.permission.SEND_SMS)
                    if (!permissionsState.callsGranted) permsToRequest.add(Manifest.permission.CALL_PHONE)
                    if (!permissionsState.notificationsGranted && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                        permsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
                    }
                    if (permsToRequest.isNotEmpty()) {
                        multiplePermissionsLauncher.launch(permsToRequest.toTypedArray())
                    }
                }, modifier = Modifier.weight(1f)) {
                    Text("Request Missing Permissions", maxLines = 1, style = MaterialTheme.typography.labelSmall)
                }
            }

            Button(onClick = { 
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.parse("package:${context.packageName}")
                }
                context.startActivity(intent)
            }, modifier = Modifier.fillMaxWidth()) {
                Text("Open App Settings")
            }
"""

# Find the end of DeveloperDashboardScreen composable
# It ends right before @Composable fun StatusItem
parts = content.split("@Composable\nfun StatusItem")

new_content = parts[0].rstrip()[:-1] + module7_code + "\n        }\n    }\n}\n\n@Composable\nfun StatusItem" + parts[1]

with open("app/src/main/java/com/example/ui/screens/DeveloperDashboardScreen.kt", "w") as f:
    f.write(new_content)
