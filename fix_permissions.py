import re

with open("app/src/main/java/com/example/MainActivity.kt", "r") as f:
    content = f.read()

bad_block = """                try {
                    com.example.service.BleForegroundService.start(context)
                } catch (e: Exception) {
                    // Ignore
                }"""

# Actually, I'll just remove that block entirely from ON_START, and only start it when permissions are fully granted.
# Wait, if we are in ON_START, and missingPermissions.isNotEmpty() is false (i.e. we have permissions), we should start it!
# So we can move it to the `else` block.

new_block = """                val missingPermissions = permissionsToRequest.toList().filter {
                    ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
                }

                if (missingPermissions.isNotEmpty()) {
                    launcher.launch(missingPermissions.toTypedArray())
                } else {
                    try {
                        com.example.service.BleForegroundService.start(context)
                    } catch (e: Exception) {
                        // Ignore
                    }
                    val locationManager = context.getSystemService(android.content.Context.LOCATION_SERVICE) as android.location.LocationManager"""

# Perform replacements
content = content.replace(bad_block, "")

# Wait, `val locationManager = ...`
content = content.replace("val locationManager = context.getSystemService(android.content.Context.LOCATION_SERVICE) as android.location.LocationManager", 
"""try { com.example.service.BleForegroundService.start(context) } catch (e: Exception) {}
                    val locationManager = context.getSystemService(android.content.Context.LOCATION_SERVICE) as android.location.LocationManager""")

with open("app/src/main/java/com/example/MainActivity.kt", "w") as f:
    f.write(content)

