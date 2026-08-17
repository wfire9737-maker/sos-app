cat app/src/main/java/com/example/ui/screens/DeviceMonitoringScreen.kt | sed 's/value = if (device.status == "DISCONNECTED") "--" else "${device.deviceTemperature}°C"/value = "--"/g' | sed 's/value = if (device.status == "DISCONNECTED") "--" else "${device.cpuUsagePercent}%"/value = "--"/g' > tmp_mon_fake2.kt
mv tmp_mon_fake2.kt app/src/main/java/com/example/ui/screens/DeviceMonitoringScreen.kt
