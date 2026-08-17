cat app/src/main/java/com/example/ui/screens/DeviceMonitoringScreen.kt | sed 's/color = if (device.batteryLevel > 20) SafetyGreen else EmergencyRed/color = if (device.status == "DISCONNECTED") Color.Gray else if (device.batteryLevel > 20) SafetyGreen else EmergencyRed/g' > tmp_monitor2.kt
mv tmp_monitor2.kt app/src/main/java/com/example/ui/screens/DeviceMonitoringScreen.kt
