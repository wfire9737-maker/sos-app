import re
with open("app/src/main/java/com/example/service/NotificationService.kt", "r") as f:
    content = f.read()

content = content.replace(
    "val enabled = kotlinx.coroutines.flow.first(settingsDataStore.criticalAlarmsEnabledFlow)",
    "val enabled = settingsDataStore.criticalAlarmsEnabledFlow.first()"
)

with open("app/src/main/java/com/example/service/NotificationService.kt", "w") as f:
    f.write(content)
