import re

with open('app/src/main/java/com/example/ui/screens/SettingsScreen.kt', 'r') as f:
    content = f.read()

# I will rewrite the Features section and the Emergency Phrases section properly.
new_sections = """            item {
                SettingsSection(title = "Voice SOS & Emergency Phrases") {
                    SettingsSwitchItem(
                        icon = Icons.Default.Mic,
                        title = "Voice SOS",
                        subtitle = if (voiceSosEnabled) (if (isSpeechActive) "Listening: Active" else "Listening: Inactive") else "Disabled",
                        checked = voiceSosEnabled,
                        onCheckedChange = { viewModel.setVoiceSosEnabled(it) }
                    )
                    if (voiceSosEnabled) {
                        SettingsItem(
                            icon = Icons.Default.FormatListBulleted,
                            title = "Configured Phrases (${wakePhrases.size})",
                            subtitle = wakePhrases.take(3).joinToString(", ") + if (wakePhrases.size > 3) "..." else "",
                            onClick = { 
                                tempPhrase = ""
                                showVoicePhraseDialog = true 
                            }
                        )
                    }
                }
            }
            item {
                SettingsSection(title = "Features") {
                    SettingsItem(
                        icon = Icons.Default.Timer,
                        title = "Safety Timer",
                        subtitle = "Set up countdown safety timers",
                        onClick = onNavigateToSafetyTimer
                    )
                    SettingsItem(
                        icon = Icons.Default.Map,
                        title = "Live Tracking Map",
                        subtitle = "View current location and responders",
                        onClick = onNavigateToMap
                    )
                    SettingsItem(
                        icon = Icons.Default.SmartToy,
                        title = "AI Assistant",
                        subtitle = "Interact with AI Emergency Dashboard",
                        onClick = onNavigateToAiScreen
                    )
                }
            }"""

old_sections = re.search(r'            item \{\s*SettingsSection\(title = "Features"\).*?onClick = onNavigateToAiScreen\s*\)\s*\}\s*\}', content, re.DOTALL)
if old_sections:
    content = content.replace(old_sections.group(0), new_sections)

# Update the dialog to allow adding multiple phrases
new_dialog = """    if (showVoicePhraseDialog) {
        AlertDialog(
            onDismissRequest = { showVoicePhraseDialog = false },
            title = { Text("Emergency Phrases") },
            text = {
                Column {
                    Text("Add a new phrase to trigger SOS:", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(16.dp))
                    androidx.compose.material3.OutlinedTextField(
                        value = tempPhrase,
                        onValueChange = { tempPhrase = it },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("New phrase") }
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Current Phrases:", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    androidx.compose.foundation.lazy.LazyColumn(
                        modifier = Modifier.heightIn(max = 200.dp)
                    ) {
                        items(wakePhrases.size) { index ->
                            val phrase = wakePhrases[index]
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(phrase, style = MaterialTheme.typography.bodyMedium)
                                IconButton(onClick = { viewModel.voiceSosService.removeWakePhrase(phrase) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (tempPhrase.isNotBlank()) {
                        viewModel.voiceSosService.addWakePhrase(tempPhrase.trim())
                        tempPhrase = ""
                    } else {
                        showVoicePhraseDialog = false
                    }
                }) {
                    Text(if (tempPhrase.isNotBlank()) "Add Phrase" else "Done")
                }
            },
            dismissButton = {
                TextButton(onClick = { showVoicePhraseDialog = false }) {
                    Text("Close")
                }
            }
        )
    }"""

old_dialog = re.search(r'    if \(showVoicePhraseDialog\) \{.*?dismissButton = \{.*?\n        \)\n    \}', content, re.DOTALL)
if old_dialog:
    content = content.replace(old_dialog.group(0), new_dialog)

with open('app/src/main/java/com/example/ui/screens/SettingsScreen.kt', 'w') as f:
    f.write(content)
