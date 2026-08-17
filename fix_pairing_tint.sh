cat app/src/main/java/com/example/ui/screens/DevicePairingScreen.kt | sed 's/tint = if (device.batteryLevel > 30) Color(0xFF4CAF50) else Color(0xFFF44336)/tint = if (device.status == "DISCONNECTED") Color.Gray else if (device.batteryLevel > 30) Color(0xFF4CAF50) else Color(0xFFF44336)/g' > tmp_pairing3.kt
mv tmp_pairing3.kt app/src/main/java/com/example/ui/screens/DevicePairingScreen.kt
