cat app/src/main/java/com/example/ui/screens/HomeScreen.kt | sed 's/val maxBattery = devices.maxOfOrNull { it.batteryLevel } ?: 0/val maxBattery = devices.filter { it.status == "CONNECTED" || it.status == "ALERTing" }.maxOfOrNull { it.batteryLevel } ?: 0/g' > tmp_home2.kt
mv tmp_home2.kt app/src/main/java/com/example/ui/screens/HomeScreen.kt
