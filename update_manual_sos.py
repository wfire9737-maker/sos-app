import os

filepath_viewmodel = "app/src/main/java/com/example/ui/GuardianViewModel.kt"
with open(filepath_viewmodel, "r") as f:
    content = f.read()

old_trigger = """    fun triggerManualSOS(lat: Double = 37.7749, lng: Double = -122.4194) {
        viewModelScope.launch {
            initiateEmergencySequence(
                triggerSource = "MANUAL",
                deviceId = "MOBILE-APP-SOS"
            )
            _uiEvents.emit(UiEvent.ShowToast("ALERT TRANSMITTED: Manual SOS Triggered!"))
            _uiEvents.emit(UiEvent.NavigateToEmergency)
        }
    }"""

new_trigger = """    fun triggerManualSOS(lat: Double = 37.7749, lng: Double = -122.4194) {
        viewModelScope.launch {
            if (emergencyService.isEmergencyActive()) {
                 emergencyService.activeEmergency.value?.let { model ->
                     emergencyService.notifyEmergencyContacts(model, isUpdate = true)
                 }
                 _uiEvents.emit(UiEvent.ShowToast("ALERT TRANSMITTED: Contacts Notified Again!"))
                 return@launch
            }
            initiateEmergencySequence(
                triggerSource = "MANUAL",
                deviceId = "MOBILE-APP-SOS"
            )
            _uiEvents.emit(UiEvent.ShowToast("ALERT TRANSMITTED: Manual SOS Triggered!"))
            _uiEvents.emit(UiEvent.NavigateToEmergency)
        }
    }"""

content = content.replace(old_trigger, new_trigger)

with open(filepath_viewmodel, "w") as f:
    f.write(content)

filepath_service = "app/src/main/java/com/example/service/EmergencyService.kt"
with open(filepath_service, "r") as f:
    content_service = f.read()

old_notify = """    private fun notifyEmergencyContacts(model: EmergencyModel) {
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

new_notify = """    fun notifyEmergencyContacts(model: EmergencyModel, isUpdate: Boolean = false) {
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
            val message = if (isUpdate) {
                "🚨 LIVE UPDATE: ${model.userName} is still in emergency. Live Location: https://maps.google.com/?q=${model.latitude},${model.longitude}"
            } else {
                "🚨 EMERGENCY SOS: ${model.userName} needs help! Location: https://maps.google.com/?q=${model.latitude},${model.longitude}"
            }
            try {
                smsManager?.sendTextMessage(contact.phone, null, message, null, null)
            } catch (e: Exception) {
                Log.e("EmergencyService", "Failed to send real SMS to ${contact.phone}: ${e.message}")
            }
            
            notificationService.addNotification(
                NotificationItem(
                    id = UUID.randomUUID().toString(),
                    title = "📞 Notified Contact: ${contact.name}",
                    body = if (isUpdate) "Real-time location SMS sent to ${contact.relationship} at ${contact.phone}." else "SMS sent to ${contact.relationship} at ${contact.phone} with emergency coordinates (${model.latitude}, ${model.longitude}).",
                    type = NotificationType.EMERGENCY
                )
            )
        }
    }"""

content_service = content_service.replace(old_notify, new_notify)

old_tracking = """    private fun startHighFrequencyLocationUpdates(emergencyId: String) {
        trackingJob?.cancel()
        trackingJob = serviceScope.launch {
            while (isActive) {
                delay(3500) // 3-5 seconds frequency (3.5s)
                val currentLoc = locationService.currentLocation.value
                val currentModel = _activeEmergency.value
                
                if (currentModel != null && currentModel.emergencyId == emergencyId) {
                    val updatedModel = currentModel.copy(
                        latitude = currentLoc.latitude,
                        longitude = currentLoc.longitude,
                        responderStatus = "LIVE LOCATION UPDATING..."
                    )
                    _activeEmergency.value = updatedModel
                    saveEmergencyToCloud(updatedModel)
                }
            }
        }
    }"""

new_tracking = """    private fun startHighFrequencyLocationUpdates(emergencyId: String) {
        trackingJob?.cancel()
        trackingJob = serviceScope.launch {
            var updateCounter = 0
            while (isActive) {
                delay(3500) // 3-5 seconds frequency (3.5s)
                val currentLoc = locationService.currentLocation.value
                val currentModel = _activeEmergency.value
                
                if (currentModel != null && currentModel.emergencyId == emergencyId) {
                    val updatedModel = currentModel.copy(
                        latitude = currentLoc.latitude,
                        longitude = currentLoc.longitude,
                        responderStatus = "LIVE LOCATION UPDATING..."
                    )
                    _activeEmergency.value = updatedModel
                    saveEmergencyToCloud(updatedModel)
                    
                    updateCounter++
                    if (updateCounter % 10 == 0) {
                        notifyEmergencyContacts(updatedModel, isUpdate = true)
                    }
                }
            }
        }
    }"""

content_service = content_service.replace(old_tracking, new_tracking)

with open(filepath_service, "w") as f:
    f.write(content_service)

