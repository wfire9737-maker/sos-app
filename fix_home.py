import re

with open("app/src/main/java/com/example/ui/screens/HomeScreen.kt", "r") as f:
    content = f.read()

content = content.replace("currentRoute: String,", "currentRoute: String = \"\",")
content = content.replace("onNavigateToHome: () -> Unit,", "onNavigateToHome: () -> Unit = {},")
content = content.replace("onNavigateToMap: () -> Unit,", "onNavigateToMap: () -> Unit = {},")
content = content.replace("onNavigateToHistory: () -> Unit,", "onNavigateToHistory: () -> Unit = {},")
content = content.replace("onNavigateToTrustedPlaces: () -> Unit,", "onNavigateToTrustedPlaces: () -> Unit = {},")
content = content.replace("onNavigateToContacts: () -> Unit,", "onNavigateToContacts: () -> Unit = {},")
content = content.replace("onNavigateToSettings: () -> Unit", "onNavigateToSettings: () -> Unit = {}")

with open("app/src/main/java/com/example/ui/screens/HomeScreen.kt", "w") as f:
    f.write(content)
