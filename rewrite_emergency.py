import re

with open('app/src/main/java/com/example/service/EmergencyService.kt', 'r') as f:
    content = f.read()

pattern = r'            // Concurrently get location and execute actions.*?            startHighFrequencyLocationUpdates\(emergencyId\)'

replacement = """            // Elevate to Foreground Service IMMEDIATELY to protect process from background restrictions
            startHighFrequencyLocationUpdates(emergencyId)

            // Independent Action: Call (Do not wait for slow GPS location!)
            launch(Dispatchers.Main) {
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED) {
                    val primaryContact = databaseService.contacts.value.firstOrNull()
                    val phoneToCall = primaryContact?.phone ?: "911"
                    Log.d("EmergencyService", "CALL_REQUESTED: Attempting background dial to $phoneToCall")
                    val callIntent = Intent(Intent.ACTION_CALL).apply {
                        data = Uri.parse("tel:$phoneToCall")
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    try {
                        context.startActivity(callIntent)
                        Log.d("EmergencyService", "CALL_STARTED: Successfully launched dialer activity.")
                    } catch (e: Exception) {
                        Log.e("EmergencyService", "CALL_FAILED: Failed to start background call activity: ${e.message}")
                    }
                } else {
                    Log.w("EmergencyService", "CALL_PERMISSION_DENIED: Cannot place call.")
                }
            }

            // Independent Action: Acquire high-accuracy location, notify Cloud and SMS
            launch {
                val highAccuracyLoc = locationService.getCurrentLocationOnce(3000)
                if (highAccuracyLoc != null) {
                    model = model.copy(
                        latitude = customLat ?: highAccuracyLoc.latitude,
                        longitude = customLng ?: highAccuracyLoc.longitude,
                        accuracy = customAccuracy ?: highAccuracyLoc.accuracy,
                        altitude = customAltitude ?: highAccuracyLoc.altitude,
                        speed = customSpeed ?: highAccuracyLoc.speed.toFloat(),
                        bearing = customBearing ?: highAccuracyLoc.bearing
                    )
                    _activeEmergency.value = model
                }
                
                // Now execute subsequent network/cloud/SMS tasks concurrently
                launch { saveEmergencyToCloud(model) }
                launch { notifyEmergencyContacts(model) }
                launch {
                    notificationService.addNotification(
                        NotificationItem(
                            id = UUID.randomUUID().toString(),
                            title = "🚨 EMERGENCY SOS ACTIVE",
                            body = "SOS triggered by $userName ($triggerType). Location broadcasting live.",
                            type = NotificationType.EMERGENCY,
                            deviceId = deviceId
                        )
                    )
                }
            }"""

new_content = re.sub(pattern, replacement, content, flags=re.DOTALL)

with open('app/src/main/java/com/example/service/EmergencyService.kt', 'w') as f:
    f.write(new_content)
