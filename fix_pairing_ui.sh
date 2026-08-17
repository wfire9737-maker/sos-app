cat app/src/main/java/com/example/ui/screens/DevicePairingScreen.kt | sed 's/value = "${device.batteryLevel}%"/value = if (device.status == "DISCONNECTED") "--" else "${device.batteryLevel}%"/g' > tmp_pairing.kt
mv tmp_pairing.kt app/src/main/java/com/example/ui/screens/DevicePairingScreen.kt
