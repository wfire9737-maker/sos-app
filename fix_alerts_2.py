import re

with open("app/src/main/java/com/example/service/DatabaseService.kt", "r") as f:
    content = f.read()

# Force replace the method body
start_idx = content.find("private fun preloadDemoAlerts()")
if start_idx != -1:
    end_idx = content.find("private fun saveAlertsListLocally", start_idx)
    content = content[:start_idx] + "private fun preloadDemoAlerts() {\n        _alerts.value = emptyList()\n    }\n\n    " + content[end_idx:]

with open("app/src/main/java/com/example/service/DatabaseService.kt", "w") as f:
    f.write(content)
