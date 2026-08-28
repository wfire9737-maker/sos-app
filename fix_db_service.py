import re

with open("app/src/main/java/com/example/service/DatabaseService.kt", "r") as f:
    content = f.read()

# Replace the broken function definition and body completely
content = re.sub(r'private fun _contacts\.value = emptyList\(\) \{.*?\n        saveContactsListLocally\(demoContacts\)\n    \}', '', content, flags=re.DOTALL)
content = re.sub(r'private fun _alerts\.value = emptyList\(\) \{.*?\n        saveAlertsListLocally\(demoAlerts\)\n    \}', '', content, flags=re.DOTALL)

with open("app/src/main/java/com/example/service/DatabaseService.kt", "w") as f:
    f.write(content)
