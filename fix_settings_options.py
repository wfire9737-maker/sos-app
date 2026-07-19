import os

filepath = "app/src/main/java/com/example/ui/screens/SettingsScreen.kt"
with open(filepath, "r") as f:
    content = f.read()

# Add states
old_states = """    var showLogoutDialog by remember { mutableStateOf(false) }"""
new_states = """    val themeMode by viewModel.themeMode.collectAsState()
    val language by viewModel.language.collectAsState()
    val notificationsEnabled by viewModel.criticalAlarmsEnabled.collectAsState()
    
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }"""

content = content.replace(old_states, new_states)

# Replace dark theme
old_dark_theme = """                    SettingsItem(
                        icon = Icons.Default.DarkMode,
                        title = "Dark Theme",
                        subtitle = "Toggle dark mode",
                        onClick = { /* TODO: Implement theme switching via ViewModel */ }
                    )"""
new_dark_theme = """                    SettingsSwitchItem(
                        icon = Icons.Default.DarkMode,
                        title = "Dark Theme",
                        subtitle = "Toggle dark mode",
                        checked = themeMode == "DARK",
                        onCheckedChange = { isDark -> viewModel.setThemeMode(if (isDark) "DARK" else "LIGHT") }
                    )"""
                    
content = content.replace(old_dark_theme, new_dark_theme)

# Replace notifications
old_notifications = """                    SettingsItem(
                        icon = Icons.Default.Notifications,
                        title = "Notifications",
                        subtitle = "Alert sounds and haptics",
                        onClick = { /* TODO */ }
                    )"""
new_notifications = """                    SettingsSwitchItem(
                        icon = Icons.Default.Notifications,
                        title = "Notifications",
                        subtitle = "Alert sounds and haptics",
                        checked = notificationsEnabled,
                        onCheckedChange = { enabled -> viewModel.setCriticalAlarmsEnabled(enabled) }
                    )"""

content = content.replace(old_notifications, new_notifications)

# Replace language
old_language = """                    SettingsItem(
                        icon = Icons.Default.Language,
                        title = "Language",
                        subtitle = "English (US)",
                        onClick = { /* TODO */ }
                    )"""
new_language = """                    SettingsItem(
                        icon = Icons.Default.Language,
                        title = "Language",
                        subtitle = when(language) { "en" -> "English (US)"; "es" -> "Español"; "fr" -> "Français"; else -> language },
                        onClick = { showLanguageDialog = true }
                    )"""

content = content.replace(old_language, new_language)

# Add SettingsSwitchItem
old_bottom = """    if (showLogoutDialog) {"""
new_bottom = """    if (showLanguageDialog) {
        AlertDialog(
            onDismissRequest = { showLanguageDialog = false },
            title = { Text("Select Language") },
            text = {
                Column {
                    listOf("en" to "English (US)", "es" to "Español", "fr" to "Français").forEach { (code, name) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.setLanguage(code)
                                    showLanguageDialog = false
                                }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = language == code,
                                onClick = {
                                    viewModel.setLanguage(code)
                                    showLanguageDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(name)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLanguageDialog = false }) { Text("Close") }
            }
        )
    }

    if (showLogoutDialog) {"""

content = content.replace(old_bottom, new_bottom)

# Add SettingsSwitchItem composable at the end
content += """

@Composable
fun SettingsSwitchItem(icon: ImageVector, title: String, subtitle: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}
"""

with open(filepath, "w") as f:
    f.write(content)

