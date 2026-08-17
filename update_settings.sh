cat app/src/main/java/com/example/ui/screens/SettingsScreen.kt | sed '/val notificationsEnabled by viewModel.criticalAlarmsEnabled/a \
    val voiceSosEnabled by viewModel.voiceSosEnabled.collectAsState()\
    val voiceSosPhrase by viewModel.voiceSosPhrase.collectAsState()\
    var showVoicePhraseDialog by remember { mutableStateOf(false) }\
    var tempPhrase by remember { mutableStateOf("") }' > tmp_settings.kt
mv tmp_settings.kt app/src/main/java/com/example/ui/screens/SettingsScreen.kt
