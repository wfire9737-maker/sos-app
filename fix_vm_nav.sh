cat app/src/main/java/com/example/ui/GuardianViewModel.kt | sed '/_uiEvents.emit(UiEvent.NavigateToEmergency)/d' > tmp_vm_nav.kt
mv tmp_vm_nav.kt app/src/main/java/com/example/ui/GuardianViewModel.kt
