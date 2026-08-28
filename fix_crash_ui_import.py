import re

with open("app/src/main/java/com/example/MainActivity.kt", "r") as f:
    content = f.read()

content = content.replace("androidx.compose.ui.Modifier.fillMaxSize().androidx.compose.foundation.background", "androidx.compose.ui.Modifier.fillMaxSize().background")

with open("app/src/main/java/com/example/MainActivity.kt", "w") as f:
    f.write(content)
