cat app/src/main/java/com/example/ui/GuardianViewModel.kt | sed '/val confirmationMsg = "🚨 Voice SOS Triggered/{
    N
    N
    c\
                    val confirmationMsg = "🚨 Voice SOS: Countdown Initiated ("${command.matchedPhrase}")"\
                    _voiceCommandConfirmation.value = confirmationMsg\
                    _uiEvents.emit(UiEvent.ShowToast(confirmationMsg))
}' > tmp_vm_voice3.kt
mv tmp_vm_voice3.kt app/src/main/java/com/example/ui/GuardianViewModel.kt
