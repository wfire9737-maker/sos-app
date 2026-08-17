import re

file_path = "app/src/main/java/com/example/service/EmergencyService.kt"

with open(file_path, "r") as f:
    content = f.read()

# Add okhttp imports if not present
if "import okhttp3.OkHttpClient" not in content:
    content = content.replace("import android.util.Log", "import android.util.Log\nimport okhttp3.OkHttpClient\nimport okhttp3.Request\nimport kotlinx.coroutines.Dispatchers\nimport kotlinx.coroutines.withContext")

# Add resetESP32 method
reset_method = """
    private fun resetESP32() {
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
    }
    
    private fun closeActiveSession() {
"""

content = content.replace("    private fun closeActiveSession() {", reset_method.strip("\n"))

close_active_session_body = """
    private fun closeActiveSession() {
        resetESP32()
"""

content = content.replace("    private fun closeActiveSession() {", close_active_session_body.strip("\n"))

with open(file_path, "w") as f:
    f.write(content)

print("Patch applied to EmergencyService.kt")
