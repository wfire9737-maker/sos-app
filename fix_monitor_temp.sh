cat app/src/main/java/com/example/ui/screens/DeviceMonitoringScreen.kt | sed 's/value = "${device.deviceTemperature}°C"/value = if (device.status == "DISCONNECTED") "--" else "${device.deviceTemperature}°C"/g' > tmp_mon_temp.kt
mv tmp_mon_temp.kt app/src/main/java/com/example/ui/screens/DeviceMonitoringScreen.kt
