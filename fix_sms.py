import os

filepath = "app/src/main/java/com/example/service/EmergencyService.kt"
with open(filepath, "r") as f:
    content = f.read()

target = """        contacts.forEach { contact ->
            val message = if (isUpdate) {
                "🚨 LIVE UPDATE: ${model.userName} is still in emergency. Live Location: https://maps.google.com/?q=${model.latitude},${model.longitude}"
            } else {
                "🚨 EMERGENCY SOS: ${model.userName} needs help! Location: https://maps.google.com/?q=${model.latitude},${model.longitude}"
            }
            try {
                smsManager?.sendTextMessage(contact.phone, null, message, null, null)"""

replacement = """        val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
        val timestamp = dateFormat.format(java.util.Date(model.startTimeMs))
        val sentPhones = mutableSetOf<String>()

        contacts.forEach { contact ->
            if (sentPhones.contains(contact.phone)) return@forEach
            sentPhones.add(contact.phone)
            
            val message = if (isUpdate) {
                "LIVE UPDATE!\n${model.userName} is still in an active emergency.\n\nLocation:\nhttps://maps.google.com/?q=${model.latitude},${model.longitude}\n\nTime: $timestamp"
            } else {
                "EMERGENCY!\n${model.userName} has triggered an SOS.\n\nLocation:\nhttps://maps.google.com/?q=${model.latitude},${model.longitude}\n\nPlease contact immediately.\n\nTime: $timestamp"
            }
            try {
                // For long SMS, we should use sendMultipartTextMessage
                val parts = smsManager?.divideMessage(message)
                if (parts != null) {
                    smsManager.sendMultipartTextMessage(contact.phone, null, parts, null, null)
                } else {
                    smsManager?.sendTextMessage(contact.phone, null, message, null, null)
                }"""

if target in content:
    content = content.replace(target, replacement)
    with open(filepath, "w") as f:
        f.write(content)
    print("Fixed SMS generation")
else:
    print("Target not found")
