cat app/src/main/java/com/example/ui/screens/HomeScreen.kt | sed 's/value = if (devices.isNotEmpty()) "$maxBattery%" else "--"/value = if (isBleConnected) "$maxBattery%" else "--"/g' > tmp_home.kt
mv tmp_home.kt app/src/main/java/com/example/ui/screens/HomeScreen.kt
