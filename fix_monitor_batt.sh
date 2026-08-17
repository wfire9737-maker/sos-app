cat app/src/main/java/com/example/ui/screens/DeviceMonitoringScreen.kt | sed 's/icon = Icons.Default.BatteryFull/icon = if (device.status == "DISCONNECTED") Icons.Default.BatteryUnknown else Icons.Default.BatteryFull/g' > tmp_monitor.kt
mv tmp_monitor.kt app/src/main/java/com/example/ui/screens/DeviceMonitoringScreen.kt
