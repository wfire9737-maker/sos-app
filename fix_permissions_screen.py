import os

filepath = "app/src/main/java/com/example/ui/screens/PermissionsScreen.kt"
with open(filepath, "r") as f:
    content = f.read()

target = """            PermissionItem(
                title = "Location",
                description = "Required to share your exact location with emergency contacts.",
                icon = Icons.Filled.LocationOn,
                isGranted = locationGranted,
                onRequest = { locationLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)) }
            )
        }"""
replacement = """            PermissionItem(
                title = "Location",
                description = "Required to share your exact location with emergency contacts.",
                icon = Icons.Filled.LocationOn,
                isGranted = locationGranted,
                onRequest = { locationLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)) }
            )

            val isOverlayGranted = android.provider.Settings.canDrawOverlays(context)
            PermissionItem(
                title = "Background Calls & Alerts",
                description = "Required to make automatic SOS calls and show alerts even when the app is closed.",
                icon = Icons.Filled.ArrowBack, // using an existing icon to avoid import issues
                isGranted = isOverlayGranted,
                onRequest = {
                    val intent = android.content.Intent(
                        android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        android.net.Uri.parse("package:${context.packageName}")
                    )
                    context.startActivity(intent)
                }
            )
        }"""

if target in content:
    content = content.replace(target, replacement)
    with open(filepath, "w") as f:
        f.write(content)
    print("Fixed PermissionsScreen")
else:
    print("Target not found")
