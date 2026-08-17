import re

file_path = "app/src/main/java/com/example/service/DeviceService.kt"

with open(file_path, "r") as f:
    content = f.read()

pattern = r"private fun startEsp32Polling\(\) \{.*?\n\s*\}\n\s*\}"

new_polling = """private fun startEsp32Polling() {
        esp32PollingJob?.cancel()
        esp32PollingJob = serviceScope.launch {
            val client = OkHttpClient.Builder()
                .connectTimeout(3, TimeUnit.SECONDS)
                .readTimeout(3, TimeUnit.SECONDS)
                .build()
               
            var wasSosActive = false
               
            while (isActive) {
                try {
                    val request = Request.Builder()
                        .url("http://10.63.183.90:8080/status?t=${System.currentTimeMillis()}")
                        .header("Cache-Control", "no-cache")
                        .build()
                       
                    client.newCall(request).execute().use { response ->
                        if (response.isSuccessful) {
                            val body = response.body?.string()
                            if (body != null) {
                                val json = JSONObject(body)
                                val isSosActive = json.optBoolean("sosActive", false)
                                   
                                if (isSosActive && !wasSosActive) {
                                    wasSosActive = true
                                    Log.d("DeviceService", "ESP32 SOS Button pressed!")
                                    addCommLog("🚨 ESP32 Hardware SOS Button Activated!")
                                    _incomingEsp32SosEvent.tryEmit("ESP32_BUTTON")
                                    handleIncomingEsp32Sos(
                                        deviceId = "ESP32-SOS-BAND-81F4",
                                        triggerType = "ESP32_BUTTON"
                                    )
                                } else if (!isSosActive && wasSosActive) {
                                    wasSosActive = false
                                    Log.d("DeviceService", "ESP32 SOS Button reset!")
                                    addCommLog("✅ ESP32 Hardware SOS Button Reset.")
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    // Suppress polling errors to avoid log spam
                }
                delay(1000) // Poll every 1 second
            }
        }
    }
}"""

content = re.sub(pattern, new_polling, content, flags=re.DOTALL)

with open(file_path, "w") as f:
    f.write(content)

print("Updated startEsp32Polling in DeviceService.kt")
