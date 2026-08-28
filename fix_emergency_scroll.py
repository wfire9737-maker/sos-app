import re

with open("app/src/main/java/com/example/ui/screens/EmergencyScreen.kt", "r") as f:
    content = f.read()

import_old = "import androidx.compose.foundation.layout.*"
import_new = "import androidx.compose.foundation.layout.*\nimport androidx.compose.foundation.rememberScrollState\nimport androidx.compose.foundation.verticalScroll"
content = content.replace(import_old, import_new)

column_old = """        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {"""

column_new = """        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {"""
content = content.replace(column_old, column_new)

with open("app/src/main/java/com/example/ui/screens/EmergencyScreen.kt", "w") as f:
    f.write(content)

