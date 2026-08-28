with open("app/src/main/java/com/example/ui/LocationPermissionHelper.kt", "r") as f:
    content = f.read()

import re
content = re.sub(
    r'val foregroundPermissionsLauncher = rememberLauncherForActivityResult\([\s\S]*?val triggerCheck = \{',
    """val foregroundPermissionsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineLoc = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        if (fineLoc) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                backgroundPermissionLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
            } else {
                onPermissionsGrantedAndGpsEnabled()
            }
        } else {
            onPermissionsGrantedAndGpsEnabled()
        }
    }

    val triggerCheck = {""",
    content
)

with open("app/src/main/java/com/example/ui/LocationPermissionHelper.kt", "w") as f:
    f.write(content)
