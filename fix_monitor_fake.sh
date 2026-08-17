cat app/src/main/java/com/example/ui/screens/DeviceMonitoringScreen.kt | sed 's/X: ${device.accelX} | Y: ${device.accelY} | Z: ${device.accelZ}/X: -- | Y: -- | Z: --/g' | sed 's/X: ${device.gyroX} | Y: ${device.gyroY} | Z: ${device.gyroZ}/X: -- | Y: -- | Z: --/g' > tmp_mon_fake.kt
mv tmp_mon_fake.kt app/src/main/java/com/example/ui/screens/DeviceMonitoringScreen.kt
