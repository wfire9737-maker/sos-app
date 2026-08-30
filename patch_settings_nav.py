import re

with open("app/src/main/java/com/example/ui/screens/SettingsScreen.kt", "r") as f:
    content = f.read()

target = """                    SettingsItem(
                        icon = Icons.Default.WifiTethering,
                        title = "Nearby Presence (BLE)","""
                        
replacement = """                    SettingsItem(
                        icon = Icons.Default.Person,
                        title = "Nearby People (Discovery)",
                        subtitle = "Scan for nearby users",
                        onClick = { onNavigateToNearbyDiscovery() }
                    )
                    
                    SettingsItem(
                        icon = Icons.Default.WifiTethering,
                        title = "Nearby Presence (BLE)","""
                        
content = content.replace(target, replacement)

# Now we need to add onNavigateToNearbyDiscovery to the function signature
target_sig = """fun SettingsScreen(
    viewModel: GuardianViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToSecurity: () -> Unit,"""
replacement_sig = """fun SettingsScreen(
    viewModel: GuardianViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToNearbyDiscovery: () -> Unit,
    onNavigateToSecurity: () -> Unit,"""
content = content.replace(target_sig, replacement_sig)

with open("app/src/main/java/com/example/ui/screens/SettingsScreen.kt", "w") as f:
    f.write(content)
