cat app/src/main/java/com/example/ui/screens/HomeScreen.kt | sed 's/val isBleConnected = devices.any { it.status == "CONNECTED" }/val isBleConnected = devices.any { it.status == "CONNECTED" || it.status == "ALERTing" }/g' > tmp_home_isble.kt
mv tmp_home_isble.kt app/src/main/java/com/example/ui/screens/HomeScreen.kt
