cat app/src/main/java/com/example/ui/screens/DeviceMonitoringScreen.kt | sed 's/color = Color(0xFF1565C0)/color = if (device.status == "DISCONNECTED") Color.Gray else Color(0xFF1565C0)/g' > tmp_monitor3.kt
mv tmp_monitor3.kt app/src/main/java/com/example/ui/screens/DeviceMonitoringScreen.kt
