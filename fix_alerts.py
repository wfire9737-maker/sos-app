import re

with open("app/src/main/java/com/example/service/DatabaseService.kt", "r") as f:
    content = f.read()

content = re.sub(r'preloadDemoAlerts\(\)', '_alerts.value = emptyList()', content)
content = re.sub(r'private fun preloadDemoAlerts\(\) \{.*?\n    \}\n', '', content, flags=re.DOTALL)

with open("app/src/main/java/com/example/service/DatabaseService.kt", "w") as f:
    f.write(content)
