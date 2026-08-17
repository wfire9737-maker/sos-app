cat app/src/main/java/com/example/service/DeviceService.kt | sed '/fun resetEsp32() {/,/^    }/c\
    fun resetEsp32() {\
        serviceScope.launch(Dispatchers.IO) {\
            try {\
                Log.d("SOS_ESP32", "RESETTING ESP32")\
                bleManager.sendCommand(com.example.ble.BleProtocol.CMD_RESET_SOS)\
                addCommLog("📡 Sent RESET command via BLE to ESP32")\
            } catch (e: Exception) {\
                Log.w("SOS_ESP32", "ESP32 RESET ERROR: ${e.message}")\
            }\
        }\
    }' > tmp_reset2.kt
mv tmp_reset2.kt app/src/main/java/com/example/service/DeviceService.kt
