import os

filepath = "app/src/main/java/com/example/MainActivity.kt"
with open(filepath, "r") as f:
    content = f.read()

target = """    val permissionsToRequest = arrayOf(
        android.Manifest.permission.SEND_SMS,
        android.Manifest.permission.CALL_PHONE,
        android.Manifest.permission.ACCESS_FINE_LOCATION,
        android.Manifest.permission.ACCESS_COARSE_LOCATION
    )"""
replacement = """    val permissionsToRequest = mutableListOf(
        android.Manifest.permission.SEND_SMS,
        android.Manifest.permission.CALL_PHONE,
        android.Manifest.permission.ACCESS_FINE_LOCATION,
        android.Manifest.permission.ACCESS_COARSE_LOCATION,
        android.Manifest.permission.READ_CONTACTS
    )
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
        permissionsToRequest.add(android.Manifest.permission.POST_NOTIFICATIONS)
    }
    """

if target in content:
    content = content.replace(target, replacement)
    
    # Also need to fix missingPermissions to use toTypedArray
    target2 = """                val missingPermissions = permissionsToRequest.filter {"""
    replacement2 = """                val missingPermissions = permissionsToRequest.toList().filter {"""
    content = content.replace(target2, replacement2)
    
    with open(filepath, "w") as f:
        f.write(content)
    print("Fixed AppPermissionChecker in MainActivity")
else:
    print("Target not found in MainActivity")

