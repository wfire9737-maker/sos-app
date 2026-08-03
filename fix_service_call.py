import os

filepath = "app/src/main/java/com/example/service/EmergencyService.kt"
with open(filepath, "r") as f:
    content = f.read()

target = """        // Make an automated real-time phone call to the first contact or 911
        val primaryContact = databaseService.contacts.value.firstOrNull()
        val phoneToCall = primaryContact?.phone ?: "911"
        val canCall = ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED
        
        // ACTION_CALL is restricted for emergency numbers like 911 on Android.
        val isEmergencyNumber = phoneToCall == "911" || phoneToCall == "112" || phoneToCall == "999"
        
        val action = if (canCall && !isEmergencyNumber) Intent.ACTION_CALL else Intent.ACTION_DIAL
        val callIntent = Intent(action).apply {
            data = Uri.parse("tel:$phoneToCall")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        try {
            context.startActivity(callIntent)
        } catch (e: Exception) {
            Log.e("EmergencyService", "Failed to initiate real-time call: ${e.message}")
            if (action == Intent.ACTION_CALL) {
                try {
                    val dialIntent = Intent(Intent.ACTION_DIAL).apply {
                        data = Uri.parse("tel:$phoneToCall")
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(dialIntent)
                } catch (e2: Exception) {}
            }
        }"""

replacement = """        // Make an automated real-time phone call to the first contact or 911
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED) {
            val primaryContact = databaseService.contacts.value.firstOrNull()
            val phoneToCall = primaryContact?.phone ?: "911"
            val callIntent = Intent(Intent.ACTION_CALL).apply {
                data = Uri.parse("tel:$phoneToCall")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            try {
                context.startActivity(callIntent)
            } catch (e: Exception) {
                Log.e("EmergencyService", "Failed to initiate real-time call: ${e.message}")
            }
        }"""

if target in content:
    content = content.replace(target, replacement)
    with open(filepath, "w") as f:
        f.write(content)
    print("Fixed EmergencyService call logic")
else:
    print("Target not found in EmergencyService")

