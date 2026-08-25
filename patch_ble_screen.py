import re

with open('app/src/main/java/com/example/ui/screens/BleTestScreen.kt', 'r') as f:
    content = f.read()

target = "fun BleTestScreen(bleManager: BleManager) {"
replacement = "fun BleTestScreen(bleManager: BleManager, onNavigateBack: () -> Unit = {}) {"
content = content.replace(target, replacement)

target_icon = "        Text(\"ESP32 BLE Connection Test\", style = MaterialTheme.typography.titleLarge)"
replacement_icon = """        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            IconButton(onClick = onNavigateBack) {
                Icon(androidx.compose.material.icons.Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Text("ESP32 BLE Connection Test", style = MaterialTheme.typography.titleLarge)
        }"""
content = content.replace(target_icon, replacement_icon)

# Add imports for Icons
if "import androidx.compose.material.icons.Icons" not in content:
    content = content.replace("import androidx.compose.material3.*", "import androidx.compose.material3.*\nimport androidx.compose.material.icons.Icons\nimport androidx.compose.material.icons.automirrored.filled.ArrowBack")

with open('app/src/main/java/com/example/ui/screens/BleTestScreen.kt', 'w') as f:
    f.write(content)
