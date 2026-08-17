cat app/src/main/java/com/example/ui/screens/DevicePairingScreen.kt | sed 's/value = healthText,/value = if (device.status == "DISCONNECTED") "--" else healthText,/g' > tmp_pairing_h.kt
mv tmp_pairing_h.kt app/src/main/java/com/example/ui/screens/DevicePairingScreen.kt
