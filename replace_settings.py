import os

filepath = "app/src/main/java/com/example/ui/screens/SettingsScreen.kt"
with open(filepath, "r") as f:
    content = f.read()

# Let's replace the signature
old_sig = """fun SettingsScreen(
    viewModel: GuardianViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToSecurity: () -> Unit,
    onNavigateToFallDetection: () -> Unit = {},
    onNavigateToAnalytics: () -> Unit = {}
)"""
new_sig = """fun SettingsScreen(
    viewModel: GuardianViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToSecurity: () -> Unit,
    onNavigateToFallDetection: () -> Unit = {},
    onNavigateToAnalytics: () -> Unit = {},
    onNavigateToVoiceSos: () -> Unit = {},
    onNavigateToSafetyTimer: () -> Unit = {},
    onNavigateToMap: () -> Unit = {},
    onNavigateToQRCode: () -> Unit = {},
    onNavigateToHelpFaq: () -> Unit = {},
    onNavigateToAiScreen: () -> Unit = {}
)"""
content = content.replace(old_sig, new_sig)

# Let's add QR Code to Account & Security
old_sec = """                SettingsSection(title = "Account & Security") {
                    SettingsItem(
                        icon = Icons.Default.Security,
                        title = "Security & PIN",
                        subtitle = "Manage emergency PIN and biometric login",
                        onClick = onNavigateToSecurity
                    )
                }"""
new_sec = """                SettingsSection(title = "Account & Security") {
                    SettingsItem(
                        icon = Icons.Default.Security,
                        title = "Security & PIN",
                        subtitle = "Manage emergency PIN and biometric login",
                        onClick = onNavigateToSecurity
                    )
                    SettingsItem(
                        icon = Icons.Default.QrCode,
                        title = "Medical QR Code",
                        subtitle = "View and share your medical information",
                        onClick = onNavigateToQRCode
                    )
                }"""
content = content.replace(old_sec, new_sec)

# Let's add Features before Device Settings
old_dev = """            item {
                SettingsSection(title = "Device Settings") {"""
new_dev = """            item {
                SettingsSection(title = "Features") {
                    SettingsItem(
                        icon = Icons.Default.Mic,
                        title = "Voice SOS",
                        subtitle = "Configure voice activation",
                        onClick = onNavigateToVoiceSos
                    )
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
            }

            item {
                SettingsSection(title = "Device Settings") {"""
content = content.replace(old_dev, new_dev)

# Add Help & FAQ
old_pref = """                    SettingsItem(
                        icon = Icons.Default.Language,
                        title = "Language",
                        subtitle = "English (US)",
                        onClick = { /* TODO */ }
                    )
                }"""
new_pref = """                    SettingsItem(
                        icon = Icons.Default.Language,
                        title = "Language",
                        subtitle = "English (US)",
                        onClick = { /* TODO */ }
                    )
                    SettingsItem(
                        icon = Icons.Default.Help,
                        title = "Help & FAQ",
                        subtitle = "Get support and read FAQs",
                        onClick = onNavigateToHelpFaq
                    )
                }"""
content = content.replace(old_pref, new_pref)

with open(filepath, "w") as f:
    f.write(content)

