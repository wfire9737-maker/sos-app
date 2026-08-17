cat app/src/main/java/com/example/ui/screens/DevicePairingScreen.kt | sed 's/device.batteryLevel > 80 -> Icons.Default.BatteryFull/device.status == "DISCONNECTED" -> Icons.Default.BatteryUnknown\n                                    device.batteryLevel > 80 -> Icons.Default.BatteryFull/g' > tmp_pairing2.kt
mv tmp_pairing2.kt app/src/main/java/com/example/ui/screens/DevicePairingScreen.kt
