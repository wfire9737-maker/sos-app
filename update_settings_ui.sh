cat app/src/main/java/com/example/ui/screens/SettingsScreen.kt | sed '/SettingsSection("Alerts & Notifications") {/a \
                    SettingsSwitchItem(\
                        icon = Icons.Default.Mic,\
                        title = "Voice SOS",\
                        subtitle = if (voiceSosEnabled) "Listening for wake phrase" else "Disabled",\
                        checked = voiceSosEnabled,\
                        onCheckedChange = { viewModel.setVoiceSosEnabled(it) }\
                    )\
                    if (voiceSosEnabled) {\
                        SettingsItem(\
                            icon = Icons.Default.TextFields,\
                            title = "Emergency Phrase",\
                            subtitle = "\"$voiceSosPhrase\"",\
                            onClick = { \
                                tempPhrase = voiceSosPhrase\
                                showVoicePhraseDialog = true \
                            }\
                        )\
                    }' > tmp_settings_ui.kt
mv tmp_settings_ui.kt app/src/main/java/com/example/ui/screens/SettingsScreen.kt
