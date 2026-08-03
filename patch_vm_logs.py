import re

with open("app/src/main/java/com/example/ui/GuardianViewModel.kt", "r") as f:
    content = f.read()

imports = """
import com.example.model.DeveloperLog
"""
if "import com.example.model.DeveloperLog" not in content:
    content = content.replace("import com.example.model.Device", "import com.example.model.Device\nimport com.example.model.DeveloperLog")

new_code = """
    // --- MODULE 8: DEVELOPER LOGS ---
    private val _developerLogs = MutableStateFlow<List<DeveloperLog>>(listOf(
        DeveloperLog(event = "App Started", status = "SUCCESS"),
        DeveloperLog(event = "Bluetooth Connected", status = "INFO"),
        DeveloperLog(event = "GPS Acquired", status = "SUCCESS")
    ))
    val developerLogs: StateFlow<List<DeveloperLog>> = _developerLogs.asStateFlow()

    fun addDeveloperLog(event: String, status: String) {
        val currentLogs = _developerLogs.value.toMutableList()
        currentLogs.add(0, DeveloperLog(event = event, status = status))
        _developerLogs.value = currentLogs
    }

    fun clearDeveloperLogs() {
        _developerLogs.value = emptyList()
    }
"""

content = content.replace("    fun deleteTestRecords() {", new_code + "\n    fun deleteTestRecords() {")

with open("app/src/main/java/com/example/ui/GuardianViewModel.kt", "w") as f:
    f.write(content)
