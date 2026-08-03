import re

with open("app/src/main/java/com/example/ui/screens/DeveloperDashboardScreen.kt", "r") as f:
    content = f.read()

content = content.replace("androidx.compose.foundation.lazy.LazyColumn(\n            modifier = Modifier\n                .fillMaxSize()", "Column(\n            modifier = Modifier\n                .fillMaxSize().verticalScroll(rememberScrollState())")

if "import androidx.compose.foundation.verticalScroll" not in content:
    content = content.replace("import androidx.compose.foundation.layout.*", "import androidx.compose.foundation.layout.*\nimport androidx.compose.foundation.verticalScroll\nimport androidx.compose.foundation.rememberScrollState")

with open("app/src/main/java/com/example/ui/screens/DeveloperDashboardScreen.kt", "w") as f:
    f.write(content)
