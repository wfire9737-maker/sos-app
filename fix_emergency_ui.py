import os

filepath = "app/src/main/java/com/example/ui/screens/EmergencyScreen.kt"
with open(filepath, "r") as f:
    content = f.read()

target = """    val emergencySession by viewModel.emergencySession.collectAsState()
    val isSirenPlaying by viewModel.isSirenPlaying.collectAsState()
    val sosSoundEnabled by viewModel.sosSoundEnabled.collectAsState()"""

replacement = """    val emergencySession by viewModel.emergencySession.collectAsState()
    val isSirenPlaying by viewModel.isSirenPlaying.collectAsState()
    val sosSoundEnabled by viewModel.sosSoundEnabled.collectAsState()
    val countdown by viewModel.countdown.collectAsState()"""

if target in content:
    content = content.replace(target, replacement)
    
    # Now replace the Warning icon and SOS ACTIVATED text
    target2 = """                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = "Warning",
                        modifier = Modifier.size(64.dp),
                        tint = if (flashWarning) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onError
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "SOS ACTIVATED",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        color = if (flashWarning) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onError
                    )
                    Text(
                        text = "Help is on the way. Stay calm.",
                        fontSize = 16.sp,
                        color = if (flashWarning) MaterialTheme.colorScheme.error.copy(alpha=0.8f) else MaterialTheme.colorScheme.onError.copy(alpha=0.8f)
                    )
                }"""
                
    replacement2 = """                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (countdown != null) {
                        Text(
                            text = countdown.toString(),
                            fontSize = 72.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onError
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "SENDING SOS...",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onError
                        )
                        Text(
                            text = "Tap CANCEL to abort.",
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onError.copy(alpha=0.8f)
                        )
                    } else {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = "Warning",
                            modifier = Modifier.size(64.dp),
                            tint = if (flashWarning) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onError
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "SOS ACTIVATED",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black,
                            color = if (flashWarning) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onError
                        )
                        Text(
                            text = "Help is on the way. Stay calm.",
                            fontSize = 16.sp,
                            color = if (flashWarning) MaterialTheme.colorScheme.error.copy(alpha=0.8f) else MaterialTheme.colorScheme.onError.copy(alpha=0.8f)
                        )
                    }
                }"""
                
    content = content.replace(target2, replacement2)
    
    # Also if it's countdown, don't require PIN to cancel
    target3 = """            Button(
                onClick = { showPinDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurfaceVariant),"""
    replacement3 = """            Button(
                onClick = { 
                    if (countdown != null) {
                        viewModel.cancelEmergencyWithPin("", "") { success -> 
                            if (success) onNavigateBack()
                        }
                    } else {
                        showPinDialog = true 
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurfaceVariant),"""
                
    content = content.replace(target3, replacement3)
    
    # We must also change GuardianViewModel.cancelEmergencyWithPin because currently it requires the correct pin!
    
    with open(filepath, "w") as f:
        f.write(content)
    print("Fixed EmergencyScreen UI")
else:
    print("Target not found")
