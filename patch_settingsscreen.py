import re

with open('app/src/main/java/com/example/ui/screens/SettingsScreen.kt', 'r') as f:
    content = f.read()

# We need to add voiceState and isSpeechRecognizerActive states to SettingsScreen
if 'val voiceState' not in content:
    content = content.replace(
        'val voiceSosEnabled by viewModel.voiceSosEnabled.collectAsState()',
        'val voiceSosEnabled by viewModel.voiceSosEnabled.collectAsState()\n    val voiceState by viewModel.voiceSosService.voiceState.collectAsState()\n    val isSpeechActive by viewModel.voiceSosService.isSpeechRecognizerActive.collectAsState()\n    val wakePhrases by viewModel.voiceSosService.wakePhrases.collectAsState()'
    )

# Now, we need to replace the "Features" Voice SOS block with the "Emergency Phrases" block.
# Current code in SettingsScreen:
#                    SettingsSwitchItem(
#                        icon = Icons.Default.Mic,
#                        title = "Voice SOS",
#                        subtitle = if (voiceSosEnabled) "Listening for wake phrase" else "Disabled",
#                        checked = voiceSosEnabled,
#                        onCheckedChange = { viewModel.setVoiceSosEnabled(it) }
#                    )
#                    if (voiceSosEnabled) {
#                        SettingsItem(
#                            icon = Icons.Default.TextFields,
#                            title = "Emergency Phrase",
#                            subtitle = "\"" + voiceSosPhrase + "\"",
#                            onClick = { 
#                                tempPhrase = voiceSosPhrase
#                                showVoicePhraseDialog = true 
#                            }
#                        )
#                    }

new_voice_block = """                    SettingsSwitchItem(
                        icon = Icons.Default.Mic,
                        title = "Voice SOS",
                        subtitle = if (voiceSosEnabled) (if (isSpeechActive) "Listening: Active" else "Listening: Inactive") else "Disabled",
                        checked = voiceSosEnabled,
                        onCheckedChange = { 
                            viewModel.setVoiceSosEnabled(it) 
                        }
                    )
                }
            }

            item {
                SettingsSection(title = "Emergency Phrases") {
                    SettingsItem(
                        icon = Icons.Default.TextFields,
                        title = "Custom Emergency Phrase",
                        subtitle = "\"" + voiceSosPhrase + "\"",
                        onClick = { 
                            tempPhrase = voiceSosPhrase
                            showVoicePhraseDialog = true 
                        }
                    )
                    SettingsItem(
                        icon = Icons.Default.FormatListBulleted,
                        title = "Fallback Voice Commands",
                        subtitle = "Help, SOS, Stop SOS, Cancel SOS, Track Location",
                        onClick = { }
                    )
"""

pattern = r'\s*SettingsSwitchItem\(\s*icon = Icons\.Default\.Mic,\s*title = "Voice SOS",.*?if \(voiceSosEnabled\) \{\s*SettingsItem\(\s*icon = Icons\.Default\.TextFields,\s*title = "Emergency Phrase",.*?\}\s*\)\s*\}'
content = re.sub(pattern, new_voice_block, content, flags=re.DOTALL)

with open('app/src/main/java/com/example/ui/screens/SettingsScreen.kt', 'w') as f:
    f.write(content)
