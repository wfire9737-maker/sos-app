import os

filepath = "app/src/main/java/com/example/service/EmergencyService.kt"
with open(filepath, "r") as f:
    content = f.read()

old_notify = """    private fun notifyEmergencyContacts(model: EmergencyModel) {
        val contacts = databaseService.contacts.value
        contacts.forEach { contact ->
            notificationService.addNotification(
                NotificationItem(
                    id = UUID.randomUUID().toString(),
                    title = "📞 Notified Contact: ${contact.name}",
                    body = "Secure SMS dispatch queued to ${contact.relationship} at ${contact.phone} with emergency coordinates (${model.latitude}, ${model.longitude}).",
                    type = NotificationType.EMERGENCY
                )
            )
        }
    }"""

new_notify = """    private fun notifyEmergencyContacts(model: EmergencyModel) {
        val contacts = databaseService.contacts.value
        val smsManager: android.telephony.SmsManager? = try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                context.getSystemService(android.telephony.SmsManager::class.java)
            } else {
                @Suppress("DEPRECATION")
                android.telephony.SmsManager.getDefault()
            }
        } catch (e: Exception) {
            @Suppress("DEPRECATION")
            android.telephony.SmsManager.getDefault()
        }

        contacts.forEach { contact ->
            val message = "🚨 EMERGENCY SOS: ${model.userName} needs help! Location: https://maps.google.com/?q=${model.latitude},${model.longitude}"
            try {
                smsManager?.sendTextMessage(contact.phone, null, message, null, null)
            } catch (e: Exception) {
                Log.e("EmergencyService", "Failed to send real SMS to ${contact.phone}: ${e.message}")
            }
            
            notificationService.addNotification(
                NotificationItem(
                    id = UUID.randomUUID().toString(),
                    title = "📞 Notified Contact: ${contact.name}",
                    body = "SMS sent to ${contact.relationship} at ${contact.phone} with emergency coordinates (${model.latitude}, ${model.longitude}).",
                    type = NotificationType.EMERGENCY
                )
            )
        }
    }"""

content = content.replace(old_notify, new_notify)

with open(filepath, "w") as f:
    f.write(content)

