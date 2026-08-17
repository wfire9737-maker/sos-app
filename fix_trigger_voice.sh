cat app/src/main/java/com/example/ui/GuardianViewModel.kt | sed '/_uiEvents.emit(UiEvent.ShowToast("🚨 VOICE SOS: AUTOMATIC SOS DISPATCHED!"))/d' > tmp_trigger.kt
mv tmp_trigger.kt app/src/main/java/com/example/ui/GuardianViewModel.kt
