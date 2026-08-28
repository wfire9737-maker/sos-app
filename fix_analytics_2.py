import re

with open("app/src/main/java/com/example/ui/screens/AnalyticsScreen.kt", "r") as f:
    content = f.read()

# I will replace the whole processedAlerts block properly.
content = re.sub(r'    val processedAlerts = remember\(realAlerts, selectedFilter, customStartDate, customEndDate\) \{.*?\n    \}\n.*?val combined = \(realAlerts\..*?\.sortedByDescending \{ it\.timestamp \}\n.*?combined\.filter \{ it\.timestamp in startMillis\.\.endMillis \}\n    \}', 
"""    val processedAlerts = remember(realAlerts, selectedFilter, customStartDate, customEndDate) {
        val now = System.currentTimeMillis()
        val calendar = Calendar.getInstance()
        
        val combined = realAlerts.sortedByDescending { it.timestamp }

        // Filter based on the selected date filter
        val startMillis = when (selectedFilter) {
            "7D" -> now - 7 * 24 * 3600 * 1000L
            "30D" -> now - 30 * 24 * 3600 * 1000L
            "MTD" -> {
                calendar.timeInMillis = now
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.timeInMillis
            }
            "CUSTOM" -> customStartDate
            else -> 0L // ALL
        }

        val endMillis = if (selectedFilter == "CUSTOM") customEndDate else now

        combined.filter { it.timestamp in startMillis..endMillis }
    }""", content, flags=re.DOTALL)

with open("app/src/main/java/com/example/ui/screens/AnalyticsScreen.kt", "w") as f:
    f.write(content)
