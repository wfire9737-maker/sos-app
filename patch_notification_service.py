import re

with open("app/src/main/java/com/example/service/NotificationService.kt", "r") as f:
    content = f.read()

content = content.replace(
    "class NotificationService(private val context: Context, private val firestore: FirebaseFirestore?) {",
    "class NotificationService(private val context: Context, private val firestore: FirebaseFirestore?, private val settingsDataStore: com.example.data.SettingsDataStore) {"
)

content = content.replace(
    "private fun showSystemNotificationBar(item: NotificationItem) {",
    """private fun showSystemNotificationBar(item: NotificationItem) {
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            val enabled = kotlinx.coroutines.flow.first(settingsDataStore.criticalAlarmsEnabledFlow)
            if (!enabled) return@launch
            showSystemNotificationBarInternal(item)
        }
    }

    private fun showSystemNotificationBarInternal(item: NotificationItem) {"""
)

content = content.replace(
    "import kotlinx.coroutines.launch",
    "import kotlinx.coroutines.launch\nimport kotlinx.coroutines.flow.first"
)

with open("app/src/main/java/com/example/service/NotificationService.kt", "w") as f:
    f.write(content)
