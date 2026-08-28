import re

with open("app/src/main/java/com/example/ui/screens/EmergencyScreen.kt", "r") as f:
    content = f.read()

list_old = """                                // Status list
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Status", color = StitchTextMuted, fontSize = 12.sp)
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = StitchGreen, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("SMS Dispatched", color = Color.White, fontSize = 14.sp)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Mic, contentDescription = null, tint = StitchPurple, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Audio Recording...", color = Color.White, fontSize = 14.sp)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Spacer(modifier = Modifier.width(24.dp))
                        Text("Emergency Services Pending", color = StitchTextMuted, fontSize = 14.sp)
                    }
                }"""

list_new = """                                // Status list
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Status", color = StitchTextMuted, fontSize = 12.sp)
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = StitchGreen, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (emergency?.contactsNotified?.isNotEmpty() == true) "SMS Dispatched" else "Dispatching SMS...", color = Color.White, fontSize = 14.sp)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.LocationOn, contentDescription = null, tint = StitchPurple, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Location Active", color = Color.White, fontSize = 14.sp)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Spacer(modifier = Modifier.width(24.dp))
                        Text(emergency?.responderStatus ?: "Emergency Services Pending", color = StitchTextMuted, fontSize = 14.sp)
                    }
                }"""

content = content.replace(list_old, list_new)

with open("app/src/main/java/com/example/ui/screens/EmergencyScreen.kt", "w") as f:
    f.write(content)
