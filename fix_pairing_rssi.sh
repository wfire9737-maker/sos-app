cat app/src/main/java/com/example/ui/screens/DevicePairingScreen.kt | sed 's/value = "${device.signalStrength} dBm"/value = if (device.status == "DISCONNECTED") "--" else "${device.signalStrength} dBm"/g' > tmp_pairing_rssi.kt
mv tmp_pairing_rssi.kt app/src/main/java/com/example/ui/screens/DevicePairingScreen.kt
