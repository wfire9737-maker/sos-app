cat app/src/main/java/com/example/ui/screens/HomeScreen.kt | sed 's/value = if (isBleConnected) "Connected" else "Not Paired",/value = if (isBleConnected) "Connected" else "Disconnected",/g' > tmp_home_isble2.kt
mv tmp_home_isble2.kt app/src/main/java/com/example/ui/screens/HomeScreen.kt
