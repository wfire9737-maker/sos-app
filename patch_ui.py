import re

with open("app/src/main/java/com/example/ui/screens/NearbyDiscoveryScreen.kt", "r") as f:
    content = f.read()

target = """                                    val timeString = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(device.lastSeen))
                                    Text(
                                        text = "Last seen: $timeString",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }"""
replacement = """                                    val timeString = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(device.lastSeen))
                                    Text(
                                        text = "Last seen: $timeString",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Spacer(modifier = Modifier.weight(1f))
                                
                                when (device.connectionState) {
                                    com.example.ble.nearby.NearbyConnectionState.DISCONNECTED -> {
                                        Button(
                                            onClick = { viewModel.nearbyBleManager.requestConnection(device.macAddress) },
                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                        ) {
                                            Text("Connect")
                                        }
                                    }
                                    com.example.ble.nearby.NearbyConnectionState.REQUESTING -> {
                                        OutlinedButton(
                                            onClick = { viewModel.nearbyBleManager.disconnect(device.macAddress) }
                                        ) {
                                            Text("Requesting...")
                                        }
                                    }
                                    com.example.ble.nearby.NearbyConnectionState.CONNECTED -> {
                                        Button(
                                            onClick = { viewModel.nearbyBleManager.disconnect(device.macAddress) },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                                        ) {
                                            Text("Connected")
                                        }
                                    }
                                }
                            }
                        }"""
content = content.replace(target, replacement)

# Add Color import if missing
if "import androidx.compose.ui.graphics.Color" not in content:
    content = content.replace("import androidx.compose.ui.Modifier", "import androidx.compose.ui.Modifier\nimport androidx.compose.ui.graphics.Color")

with open("app/src/main/java/com/example/ui/screens/NearbyDiscoveryScreen.kt", "w") as f:
    f.write(content)
