import re

file_path = "app/src/main/java/com/example/service/DeviceService.kt"

with open(file_path, "r") as f:
    content = f.read()

# Add imports
imports_to_add = """
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit
"""

if "import okhttp3.OkHttpClient" not in content:
    content = content.replace("import android.util.Log", "import android.util.Log\n" + imports_to_add)

# Add polling job variable
polling_var = """
    private var telemetryJob: Job? = null
    private var esp32PollingJob: Job? = null
"""

content = content.replace("private var telemetryJob: Job? = null", polling_var)

# Add startEsp32Polling in init block
init_block = """
    init {
        startTelemetryLoop()
        startConnectivityMonitors()
        startEsp32Polling()
    }
"""

content = content.replace("init {\n        startTelemetryLoop()\n        startConnectivityMonitors()\n    }", init_block.strip())

cleanup_block = """
    fun cleanup() {
        telemetryJob?.cancel()
        esp32PollingJob?.cancel()
"""

content = content.replace("    fun cleanup() {\n        telemetryJob?.cancel()", cleanup_block.strip("\n"))

# Add polling logic
polling_logic = """
    private fun startEsp32Polling() {
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
    }
"""

content += polling_logic

with open(file_path, "w") as f:
    f.write(content)

print("Patch applied to DeviceService.kt")
