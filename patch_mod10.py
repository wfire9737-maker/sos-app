import re

with open("app/src/main/java/com/example/ui/screens/DeveloperDashboardScreen.kt", "r") as f:
    content = f.read()

imports = """
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material3.ButtonDefaults
"""

if "import androidx.compose.material3.AlertDialog" not in content:
    content = content.replace("import androidx.compose.material3.*", imports + "import androidx.compose.material3.*")


dialog_code = """
    var showResetDialog by remember { mutableStateOf(false) }
    var resetAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    var resetTitle by remember { mutableStateOf("") }
    var resetMessage by remember { mutableStateOf("") }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text(resetTitle) },
            text = { Text(resetMessage) },
            confirmButton = {
                TextButton(onClick = { 
                    resetAction?.invoke() 
                    showResetDialog = false 
                }) {
                    Text("Confirm", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
"""

mod10_code = """
            Spacer(modifier = Modifier.height(24.dp))
            Text("Module 10 - Reset & Exit", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { 
                        resetTitle = "Reset Simulations"
                        resetMessage = "Are you sure you want to disconnect simulated devices?"
                        resetAction = {
                            devices.forEach { viewModel.disconnectSimulatedDevice(it.deviceId) }
                        }
                        showResetDialog = true
                    }, 
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Reset All Simulations", maxLines = 1, style = MaterialTheme.typography.labelSmall)
                }
                
                Button(
                    onClick = { 
                        resetTitle = "Clear Test Data"
                        resetMessage = "Are you sure you want to delete all test records?"
                        resetAction = {
                            viewModel.deleteTestRecords()
                            viewModel.clearDeveloperLogs()
                            viewModel.cleanDiagnosticsLog()
                            viewModel.clearCommLogs()
                        }
                        showResetDialog = true
                    }, 
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Clear Test Data", maxLines = 1, style = MaterialTheme.typography.labelSmall)
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { 
                        resetTitle = "Disable Developer Mode"
                        resetMessage = "Are you sure you want to disable Developer Mode and return?"
                        resetAction = {
                            onNavigateBack()
                        }
                        showResetDialog = true
                    }, 
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Disable Developer Mode", maxLines = 1, style = MaterialTheme.typography.labelSmall)
                }
                
                Button(
                    onClick = { 
                        resetTitle = "Restart Test Environment"
                        resetMessage = "Are you sure you want to restart the test environment?"
                        resetAction = {
                            devices.forEach { viewModel.disconnectSimulatedDevice(it.deviceId) }
                            viewModel.deleteTestRecords()
                            viewModel.clearDeveloperLogs()
                            viewModel.cleanDiagnosticsLog()
                            viewModel.clearCommLogs()
                        }
                        showResetDialog = true
                    }, 
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Restart Test Environment", maxLines = 1, style = MaterialTheme.typography.labelSmall)
                }
            }
"""

content = content.replace("    val context = LocalContext.current", "    val context = LocalContext.current\n" + dialog_code)

parts = content.split("@Composable\nfun StatusItem(title: String, isOk: Boolean, statusText: String) {")
new_content = parts[0].rstrip()[:-1] + mod10_code + "\n        }\n    }\n}\n\n@Composable\nfun StatusItem(title: String, isOk: Boolean, statusText: String) {" + parts[1]


with open("app/src/main/java/com/example/ui/screens/DeveloperDashboardScreen.kt", "w") as f:
    f.write(new_content)
