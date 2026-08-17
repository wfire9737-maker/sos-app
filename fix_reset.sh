cat app/src/main/java/com/example/service/DeviceService.kt | sed '/Log.d("SOS_ESP32", "RESETTING ESP32")/,/}/c\
                Log.d("SOS_ESP32", "RESETTING ESP32")\
                bleManager.sendCommand(com.example.ble.BleProtocol.CMD_RESET_SOS)\
                addCommLog("📡 Sent RESET command via BLE to ESP32")\
' > tmp_reset.kt
mv tmp_reset.kt app/src/main/java/com/example/service/DeviceService.kt
