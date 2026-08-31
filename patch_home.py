import re

with open("app/src/main/java/com/example/ui/screens/HomeScreen.kt", "r") as f:
    content = f.read()

target_sig = """    onNavigateToBleTest: () -> Unit = {},
    onNavigateToEmergency: () -> Unit,"""
replacement_sig = """    onNavigateToBleTest: () -> Unit = {},
    onNavigateToNearbyDiscovery: () -> Unit = {},
    onNavigateToEmergency: () -> Unit,"""
content = content.replace(target_sig, replacement_sig)

target_ui = """            // Quick Status Grid
            item {
                StatusGrid(devices = devices, onBluetoothClick = onNavigateToBleTest)
            }"""
replacement_ui = """            // Quick Status Grid
            item {
                StatusGrid(devices = devices, onBluetoothClick = onNavigateToBleTest)
            }
            
            // Nearby Discovery Section
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().clickable { onNavigateToNearbyDiscovery() },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(MaterialTheme.colorScheme.primary, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.PersonSearch, contentDescription = "Nearby People", tint = MaterialTheme.colorScheme.onPrimary)
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text("Nearby People", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                            Text("Find and connect with nearby users", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f))
                        }
                    }
                }
            }"""
content = content.replace(target_ui, replacement_ui)

with open("app/src/main/java/com/example/ui/screens/HomeScreen.kt", "w") as f:
    f.write(content)
