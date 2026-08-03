with open("app/src/main/java/com/example/ui/GuardianViewModel.kt", "r") as f:
    content = f.read()

old_code = """    fun triggerManualSOS(lat: Double = 37.7749, lng: Double = -122.4194) {
        viewModelScope.launch {
            if (emergencyService.isEmergencyActive()) {"""

new_code = """    fun triggerManualSOS(lat: Double = 37.7749, lng: Double = -122.4194) {
        viewModelScope.launch {
            if (_sosWorkflowState.value != com.example.model.SosWorkflowState.IDLE && _sosWorkflowState.value != com.example.model.SosWorkflowState.COMPLETED && _sosWorkflowState.value != com.example.model.SosWorkflowState.ERROR) {
                return@launch
            }
            if (emergencyService.isEmergencyActive()) {"""

content = content.replace(old_code, new_code)

with open("app/src/main/java/com/example/ui/GuardianViewModel.kt", "w") as f:
    f.write(content)
