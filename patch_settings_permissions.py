import re

with open("app/src/main/java/com/example/ui/screens/SettingsScreen.kt", "r") as f:
    content = f.read()

# Add permission handling imports
if "androidx.activity.compose.rememberLauncherForActivityResult" not in content:
    content = content.replace("import androidx.compose.runtime.*", "import androidx.compose.runtime.*\nimport androidx.activity.compose.rememberLauncherForActivityResult\nimport androidx.activity.result.contract.ActivityResultContracts\nimport android.Manifest\nimport android.content.pm.PackageManager\nimport androidx.core.content.ContextCompat\nimport android.os.Build")

# We'll inject the permission launcher and permission check logic before the Scaffold
launcher_code = """    var showDeveloperWarningDialog by remember { mutableStateOf(false) }

    val nearbyPermissions = mutableListOf<String>()
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        nearbyPermissions.add(Manifest.permission.BLUETOOTH_ADVERTISE)
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        nearbyPermissions.add(Manifest.permission.POST_NOTIFICATIONS)
    }

    var pendingNearbyInterval by remember { mutableStateOf<Int?>(null) }
    
    val nearbyPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.entries.all { it.value }
        if (allGranted && pendingNearbyInterval != null) {
            nearbyPresenceInterval = pendingNearbyInterval!!
            prefs.edit().putInt("nearby_presence_interval", nearbyPresenceInterval).apply()
            com.example.service.NearbyBleService.startOrStop(context)
        } else {
            // Permission denied, fail gracefully (do not apply interval, it remains at current)
            pendingNearbyInterval = null
        }
    }
"""

content = content.replace("    var showDeveloperWarningDialog by remember { mutableStateOf(false) }", launcher_code)

# Replace the onClick logic to use permissions
target_click = """                        onClick = {
                            val currentIndex = presenceOptions.indexOf(nearbyPresenceInterval)
                            val nextIndex = (currentIndex + 1) % presenceOptions.size
                            val nextVal = presenceOptions[nextIndex]
                            nearbyPresenceInterval = nextVal
                            prefs.edit().putInt("nearby_presence_interval", nextVal).apply()
                            com.example.service.NearbyBleService.startOrStop(context)
                        }"""

replacement_click = """                        onClick = {
                            val currentIndex = presenceOptions.indexOf(nearbyPresenceInterval)
                            val nextIndex = (currentIndex + 1) % presenceOptions.size
                            val nextVal = presenceOptions[nextIndex]
                            
                            if (nextVal == 0) {
                                nearbyPresenceInterval = nextVal
                                prefs.edit().putInt("nearby_presence_interval", nextVal).apply()
                                com.example.service.NearbyBleService.startOrStop(context)
                            } else {
                                val hasAdvertise = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                    ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_ADVERTISE) == PackageManager.PERMISSION_GRANTED
                                } else true
                                val hasNotification = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
                                } else true

                                if (hasAdvertise && hasNotification) {
                                    nearbyPresenceInterval = nextVal
                                    prefs.edit().putInt("nearby_presence_interval", nextVal).apply()
                                    com.example.service.NearbyBleService.startOrStop(context)
                                } else {
                                    pendingNearbyInterval = nextVal
                                    nearbyPermissionLauncher.launch(nearbyPermissions.toTypedArray())
                                }
                            }
                        }"""

content = content.replace(target_click, replacement_click)

with open("app/src/main/java/com/example/ui/screens/SettingsScreen.kt", "w") as f:
    f.write(content)
