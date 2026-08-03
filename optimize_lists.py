import os

files_to_update = {
    "app/src/main/java/com/example/ui/screens/ContactsScreen.kt": [
        ("items(contacts) { contact ->", "items(contacts, key = { it.id }) { contact ->")
    ],
    "app/src/main/java/com/example/ui/screens/HomeScreen.kt": [
        ("items(alerts.filter { it.status == \"ACTIVE\" }) { alert ->", "items(alerts.filter { it.status == \"ACTIVE\" }, key = { it.id }) { alert ->"),
        ("items(devices) { device ->", "items(devices, key = { it.deviceId }) { device ->")
    ],
    "app/src/main/java/com/example/ui/screens/FallDetectionScreen.kt": [
        ("items(allEvents) { event ->", "items(allEvents, key = { it.id }) { event ->")
    ],
    "app/src/main/java/com/example/ui/screens/VoiceSosScreen.kt": [
        ("items(logs) { log ->", "items(logs, key = { it.timestamp }) { log ->")
    ],
    "app/src/main/java/com/example/ui/screens/ReportsScreen.kt": [
        ("items(processedAlerts) { alert ->", "items(processedAlerts, key = { it.id }) { alert ->")
    ],
    "app/src/main/java/com/example/ui/screens/HelpFaqScreen.kt": [
        ("items(faqs) { faq ->", "items(faqs, key = { it.question }) { faq ->")
    ]
}

for filepath, replacements in files_to_update.items():
    if os.path.exists(filepath):
        with open(filepath, "r") as f:
            content = f.read()
        
        modified = False
        for target, replacement in replacements:
            if target in content:
                content = content.replace(target, replacement)
                modified = True
                
        if modified:
            with open(filepath, "w") as f:
                f.write(content)
            print(f"Optimized {filepath}")
