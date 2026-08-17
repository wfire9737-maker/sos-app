cat app/src/main/java/com/example/ui/screens/DeviceMonitoringScreen.kt | sed 's/value = "${device.cpuUsagePercent}%"/value = if (device.status == "DISCONNECTED") "--" else "${device.cpuUsagePercent}%"/g' > tmp_mon_cpu.kt
mv tmp_mon_cpu.kt app/src/main/java/com/example/ui/screens/DeviceMonitoringScreen.kt
