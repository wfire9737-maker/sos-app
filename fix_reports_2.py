import re

with open("app/src/main/java/com/example/ui/screens/ReportsScreen.kt", "r") as f:
    content = f.read()

# I will replace the whole processedAlerts block properly.
content = re.sub(r'    val processedAlerts = remember\(realAlerts, selectedFilter, customStartDate, customEndDate\) \{.*?\n    \}\n.*?val combined = \(realAlerts.*?\.sortedByDescending \{ it\.timestamp \}\n.*?combined\.filter \{ it\.timestamp in startMillis\.\.endMillis \}\n    \}', 
"""    val processedAlerts = remember(realAlerts, selectedFilter, customStartDate, customEndDate) {
        val now = System.currentTimeMillis()
        val calendar = Calendar.getInstance()
        
        val combined = realAlerts.sortedByDescending { it.timestamp }

        // Filter based on the selected date filter
        val startMillis = when (selectedFilter) {
            "7D" -> now - 7 * 24 * 3600 * 1000L
            "30D" -> now - 30 * 24 * 3600 * 1000L
            "CUSTOM" -> customStartDate
            else -> 0L // ALL
        }

        val endMillis = if (selectedFilter == "CUSTOM") customEndDate else now

        combined.filter { it.timestamp in startMillis..endMillis }
    }""", content, flags=re.DOTALL)

with open("app/src/main/java/com/example/ui/screens/ReportsScreen.kt", "w") as f:
    f.write(content)
