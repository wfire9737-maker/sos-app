cat app/src/main/java/com/example/ui/GuardianViewModel.kt | sed '/fun handleVoiceCommand/,/                    _uiEvents.emit(UiEvent.ShowToast(confirmationMsg))/c\
    fun handleVoiceCommand(command: com.example.service.VoiceCommand, confidence: Int) {\
        viewModelScope.launch {\
            when (command) {\
                is com.example.service.VoiceCommand.Sos -> {\
                    triggerVoiceSOS(command.matchedPhrase, confidence)\
                    val confirmationMsg = "🚨 Voice SOS: Countdown Initiated (\\\"${command.matchedPhrase}\\\")"\
                    _voiceCommandConfirmation.value = confirmationMsg\
                    _uiEvents.emit(UiEvent.ShowToast(confirmationMsg))' > tmp_vm_voice5.kt
mv tmp_vm_voice5.kt app/src/main/java/com/example/ui/GuardianViewModel.kt
