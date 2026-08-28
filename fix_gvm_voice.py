import re

with open("app/src/main/java/com/example/ui/GuardianViewModel.kt", "r") as f:
    content = f.read()

init_old = """    init {
        // Start service if enabled on boot/init
        if (_voiceSosEnabled.value) {
            val intent = android.content.Intent(getApplication(), com.example.service.VoiceSosForegroundService::class.java)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                getApplication<android.app.Application>().startForegroundService(intent)
            } else {
                getApplication<android.app.Application>().startService(intent)
            }
        }
    }"""

init_new = """    init {
        // Start service if enabled on boot/init
        if (_voiceSosEnabled.value) {
            val intent = android.content.Intent(getApplication(), com.example.service.VoiceSosForegroundService::class.java)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                getApplication<android.app.Application>().startForegroundService(intent)
            } else {
                getApplication<android.app.Application>().startService(intent)
            }
        }
        
        voiceSosService.onVoiceCommandRecognized = { command, confidence ->
            if (command is com.example.service.VoiceCommand.TriggerSos) {
                triggerVoiceSOS(command.spokenText, confidence)
                viewModelScope.launch {
                    _uiEvents.emit(UiEvent.ShowToast("Voice SOS Triggered!"))
                }
            } else if (command is com.example.service.VoiceCommand.CancelSos) {
                if (emergencyService.countdown.value != null || emergencyService.activeEmergency.value != null) {
                    cancelEmergencyWithPin("") {}
                    viewModelScope.launch {
                        _uiEvents.emit(UiEvent.ShowToast("SOS Cancelled via Voice"))
                    }
                }
            }
        }
    }"""

content = content.replace(init_old, init_new)

with open("app/src/main/java/com/example/ui/GuardianViewModel.kt", "w") as f:
    f.write(content)
