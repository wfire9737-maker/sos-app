import os

filepath = "app/src/main/java/com/example/ui/screens/EmergencyScreen.kt"
with open(filepath, "r") as f:
    content = f.read()

target = """                        when (PackageManager.PERMISSION_GRANTED) {
                            ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) -> {
                                val intent = Intent(Intent.ACTION_CALL).apply { data = Uri.parse("tel:$primaryContactPhone") }
                                context.startActivity(intent)
                            }
                            else -> {
                                callPermissionLauncher.launch(Manifest.permission.CALL_PHONE)
                            }
                        }"""

replacement = """                        val isEmergencyNumber = primaryContactPhone == "911" || primaryContactPhone == "112" || primaryContactPhone == "999"
                        when {
                            isEmergencyNumber -> {
                                val intent = Intent(Intent.ACTION_DIAL).apply { data = Uri.parse("tel:$primaryContactPhone") }
                                context.startActivity(intent)
                            }
                            ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED -> {
                                try {
                                    val intent = Intent(Intent.ACTION_CALL).apply { data = Uri.parse("tel:$primaryContactPhone") }
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    val intent = Intent(Intent.ACTION_DIAL).apply { data = Uri.parse("tel:$primaryContactPhone") }
                                    context.startActivity(intent)
                                }
                            }
                            else -> {
                                callPermissionLauncher.launch(Manifest.permission.CALL_PHONE)
                            }
                        }"""

if target in content:
    content = content.replace(target, replacement)
    with open(filepath, "w") as f:
        f.write(content)
    print("Fixed EmergencyScreen call button")
else:
    print("Target not found in EmergencyScreen")
