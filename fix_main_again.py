with open("app/src/main/java/com/example/MainActivity.kt", "r") as f:
    content = f.read()

bad_block = """    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        try {
            com.example.service.BleForegroundService.start(context)
        } catch (e: Exception) {
            // Ignore
        }
        try { com.example.service.BleForegroundService.start(context) } catch (e: Exception) {}"""

good_block = """    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.entries.all { it.value }
        if (allGranted || (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S && 
            androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED)) {
            try {
                com.example.service.BleForegroundService.start(context)
            } catch (e: Exception) {
                // Ignore
            }
        }"""

content = content.replace(bad_block, good_block)

with open("app/src/main/java/com/example/MainActivity.kt", "w") as f:
    f.write(content)
