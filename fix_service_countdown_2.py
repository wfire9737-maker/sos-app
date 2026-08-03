import os
import re

filepath = "app/src/main/java/com/example/service/EmergencyService.kt"
with open(filepath, "r") as f:
    content = f.read()

target = """        val model = EmergencyModel(
            emergencyId = emergencyId,
            userId = userId,
            userName = userName,
            userPhone = userPhone,
            startTimeMs = System.currentTimeMillis(),
            latitude = lat,
            longitude = lng,
            status = "ACTIVE",
            triggerType = triggerType,
            aiConfidenceScore = if (triggerType == "FALL_DETECTED") 96 else 90,
            contactsNotified = databaseService.contacts.value.map { "${it.name} (${it.phone})" },
            responderStatus = "SOS TRIGGERED - BROADCASTING",
            deviceId = deviceId
        )

        _activeEmergency.value = model

        // Record event & timestamp in Firestore
        saveEmergencyToCloud(model)

        // Send push notification
        notificationService.addNotification(
            NotificationItem(
                id = UUID.randomUUID().toString(),
                title = "🚨 EMERGENCY SOS ACTIVE",
                body = "SOS triggered by $userName ($triggerType). Location broadcasting live.",
                type = NotificationType.EMERGENCY,
                deviceId = deviceId
            )
        )

        // Notify emergency contacts
        notifyEmergencyContacts(model)
        
        // Make an automated real-time phone call to the first contact or 911
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
        }

        // Start updating location every 3-5 seconds
        startHighFrequencyLocationUpdates(emergencyId)

        return model"""

replacement = """        val pendingModel = EmergencyModel(
            emergencyId = emergencyId,
            userId = userId,
            userName = userName,
            userPhone = userPhone,
            startTimeMs = System.currentTimeMillis(),
            latitude = lat,
            longitude = lng,
            status = "COUNTDOWN",
            triggerType = triggerType,
            aiConfidenceScore = if (triggerType == "FALL_DETECTED") 96 else 90,
            contactsNotified = databaseService.contacts.value.map { "${it.name} (${it.phone})" },
            responderStatus = "COUNTDOWN ACTIVE",
            deviceId = deviceId
        )
        
        _activeEmergency.value = pendingModel
        
        countdownJob = serviceScope.launch {
            for (i in 5 downTo 1) {
                _countdown.value = i
                delay(1000)
            }
            _countdown.value = null
            
            val model = pendingModel.copy(status = "ACTIVE", responderStatus = "SOS TRIGGERED - BROADCASTING")
            _activeEmergency.value = model

            // Record event & timestamp in Firestore
            saveEmergencyToCloud(model)

            // Send push notification
            notificationService.addNotification(
                NotificationItem(
                    id = UUID.randomUUID().toString(),
                    title = "🚨 EMERGENCY SOS ACTIVE",
                    body = "SOS triggered by $userName ($triggerType). Location broadcasting live.",
                    type = NotificationType.EMERGENCY,
                    deviceId = deviceId
                )
            )

            // Notify emergency contacts
            notifyEmergencyContacts(model)
            
            // Make an automated real-time phone call to the first contact or 911
            withContext(Dispatchers.Main) {
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
                }
            }

            // Start updating location every 3-5 seconds
            startHighFrequencyLocationUpdates(emergencyId)
        }

        return pendingModel"""

if target in content:
    content = content.replace(target, replacement)
    with open(filepath, "w") as f:
        f.write(content)
    print("Fixed startEmergency body")
else:
    print("Target not found")
