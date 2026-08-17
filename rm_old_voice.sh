cat app/src/main/java/com/example/ui/screens/SettingsScreen.kt | sed '/onClick = onNavigateToVoiceSos/{
    s/.*//
    N
    N
}' | sed '/title = "Voice SOS",/,+2d' | sed '/icon = Icons.Default.Mic,/d' | sed '/SettingsItem(/d' > tmp_rm.kt
