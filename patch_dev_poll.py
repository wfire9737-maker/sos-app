import re

file_path = "app/src/main/java/com/example/service/DeviceService.kt"

with open(file_path, "r") as f:
    content = f.read()

old_polling = """    private fun startEsp32Polling() {
        esp32PollingJob?.cancel()
        esp32PollingJob = serviceScope.launch {
            val client = OkHttpClient.Builder()
                .connectTimeout(3, TimeUnit.SECONDS)
                .readTimeout(3, TimeUnit.SECONDS)
                .build()
               
            var wasSosActive = false
               
            while (isActive) {
                if (_isNetworkAvailable.value) {
                    try {
                        val request = Request.Builder()
                            .url("http://10.63.183.90:8080/status")
                            .build()
                               
                        client.newCall(request).execute().use { response ->
                            if (response.isSuccessful) {
                                val body = response.body?.string()
                                if (body != null) {
                                    val json = JSONObject(body)
                                    val isSosActive = json.optBoolean("sosActive", false)
                                       
                                    if (isSosActive && !wasSosActive) {
                                        wasSosActive = true
                                        // Hardware SOS button pressed!
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
                        // Suppress polling errors to avoid log spam, network might be down
                    }
                }
                delay(2000) // Poll every 2 seconds
            }
        }
    }"""

new_polling = """    private fun startEsp32Polling() {
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
                    // Suppress polling errors to avoid log spam, network might be down
                }
                delay(1000) // Poll every 1 second
            }
        }
    }"""

if old_polling in content:
    content = content.replace(old_polling, new_polling)
    with open(file_path, "w") as f:
        f.write(content)
    print("Replaced polling in DeviceService.kt successfully.")
else:
    print("Old polling block not matched exactly, checking regex...")
