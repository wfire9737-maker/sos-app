cat app/src/main/java/com/example/ui/GuardianViewModel.kt | sed '/_uiEvents.emit(UiEvent.ShowToast("🚨 ESP32 SOS ALERT TRIGGERED!"))/d' | sed '/_uiEvents.emit(UiEvent.NavigateToEmergency)/d' > tmp_vm.kt
mv tmp_vm.kt app/src/main/java/com/example/ui/GuardianViewModel.kt
