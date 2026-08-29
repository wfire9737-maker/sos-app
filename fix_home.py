import os

filepath = "app/src/main/java/com/example/ui/screens/HomeScreen.kt"
with open(filepath, "r") as f:
    content = f.read()

# Change signature of HomeScreen
old_sig = """    onNavigateToSettings: () -> Unit,
    onNavigateToAnalytics: () -> Unit,
    onNavigateToReports: () -> Unit
)"""
new_sig = """    onNavigateToSettings: () -> Unit,
    onNavigateToAnalytics: () -> Unit,
    onNavigateToReports: () -> Unit,
    onNavigateToSafeCheckIn: () -> Unit = {}
)"""
content = content.replace(old_sig, new_sig)

# Add Safe Check In quick action
old_qa = """            QuickActionItem(icon = Icons.Default.Assessment, label = "Reports", onClick = onNavigateToReports)"""
new_qa = """            QuickActionItem(icon = Icons.Default.Assessment, label = "Reports", onClick = onNavigateToReports)
            QuickActionItem(icon = Icons.Default.CheckCircle, label = "Safe Check-In", onClick = onNavigateToSafeCheckIn)"""
content = content.replace(old_qa, new_qa)

with open(filepath, "w") as f:
    f.write(content)
