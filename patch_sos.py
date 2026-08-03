with open("app/src/main/java/com/example/ui/GuardianViewModel.kt", "r") as f:
    content = f.read()

import re
old_func_pattern = r"    fun triggerManualSOS\(lat: Double = 37\.7749, lng: Double = -122\.4194\) \{.*?(?=    fun triggerFallDetectedSOS\(\) \{)"

new_func = """    fun triggerManualSOS(lat: Double = 37.7749, lng: Double = -122.4194) {
        viewModelScope.launch {
            if (_sosWorkflowState.value != com.example.model.SosWorkflowState.IDLE && _sosWorkflowState.value != com.example.model.SosWorkflowState.COMPLETED) {
                return@launch
            }
            if (emergencyService.isEmergencyActive()) {
                 emergencyService.activeEmergency.value?.let { model ->
                     emergencyService.notifyEmergencyContacts(model, isUpdate = true)
                 }
                 _uiEvents.emit(UiEvent.ShowToast("ALERT TRANSMITTED: Contacts Notified Again!"))
                 return@launch
            }
            
            _sosWorkflowState.value = com.example.model.SosWorkflowState.IDLE
            
            initiateEmergencySequence(
                triggerSource = "MANUAL",
                deviceId = "MOBILE-APP-SOS"
            )
            
            _uiEvents.emit(UiEvent.NavigateToEmergency)
        }
    }

"""

new_content = re.sub(old_func_pattern, new_func, content, flags=re.DOTALL)

with open("app/src/main/java/com/example/ui/GuardianViewModel.kt", "w") as f:
    f.write(new_content)
