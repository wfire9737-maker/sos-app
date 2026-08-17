cat app/src/main/java/com/example/ui/screens/SettingsScreen.kt | sed '/if (showLanguageDialog) {/i \
    if (showVoicePhraseDialog) {\
        AlertDialog(\
            onDismissRequest = { showVoicePhraseDialog = false },\
            title = { Text("Emergency Phrase") },\
            text = {\
                Column {\
                    Text("Enter the phrase to trigger SOS:", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)\
                    Spacer(modifier = Modifier.height(16.dp))\
                    androidx.compose.material3.OutlinedTextField(\
                        value = tempPhrase,\
                        onValueChange = { tempPhrase = it },\
                        singleLine = true,\
                        modifier = Modifier.fillMaxWidth()\
                    )\
                }\
            },\
            confirmButton = {\
                Button(onClick = {\
                    if (tempPhrase.isNotBlank()) {\
                        viewModel.setVoiceSosPhrase(tempPhrase.trim())\
                    }\
                    showVoicePhraseDialog = false\
                }) {\
                    Text("Save")\
                }\
            },\
            dismissButton = {\
                TextButton(onClick = { showVoicePhraseDialog = false }) { Text("Cancel") }\
            }\
        )\
    }' > tmp_settings_dialog.kt
mv tmp_settings_dialog.kt app/src/main/java/com/example/ui/screens/SettingsScreen.kt
