sed -i 's/"Recognized Command: "$customPhrase" (Emergency SOS)"/"Recognized Command: \\"$customPhrase\\" (Emergency SOS)"/g' app/src/main/java/com/example/service/VoiceSosService.kt
sed -i 's/application\.getSharedPreferences/getApplication<Application>().getSharedPreferences/g' app/src/main/java/com/example/ui/GuardianViewModel.kt
