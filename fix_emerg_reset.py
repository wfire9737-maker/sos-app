file_path = "app/src/main/java/com/example/service/EmergencyService.kt"

with open(file_path, "r") as f:
    content = f.read()

old_reset = """    private fun resetESP32() {
        serviceScope.launch(Dispatchers.IO) {
            try {
                val client = OkHttpClient()
                val request = Request.Builder()
                    .url("http://10.63.183.90:8080/reset")
                    .build()
                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        Log.d("EmergencyService", "Successfully reset ESP32 state.")
                    }
                }
            } catch (e: Exception) {
                Log.e("EmergencyService", "Failed to reset ESP32 state", e)
            }
        }
    }"""

new_reset = """    private fun resetESP32() {
        serviceScope.launch(Dispatchers.IO) {
            try {
                val client = OkHttpClient()
                val request = Request.Builder()
                    .url("http://10.63.183.90:8080/reset?t=${System.currentTimeMillis()}")
                    .header("Cache-Control", "no-cache")
                    .build()
                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        Log.d("EmergencyService", "Successfully reset ESP32 state.")
                    }
                }
            } catch (e: Exception) {
                Log.e("EmergencyService", "Failed to reset ESP32 state", e)
            }
        }
    }"""

if old_reset in content:
    content = content.replace(old_reset, new_reset)
    with open(file_path, "w") as f:
        f.write(content)
    print("Updated resetESP32 in EmergencyService.kt")
else:
    print("Could not match exact reset block")
