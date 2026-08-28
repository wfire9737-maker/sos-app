import re

with open("app/src/main/java/com/example/ui/screens/HomeScreen.kt", "r") as f:
    content = f.read()

import_old = "import androidx.compose.foundation.layout.*"
import_new = "import androidx.compose.foundation.layout.*\nimport androidx.compose.foundation.rememberScrollState\nimport androidx.compose.foundation.verticalScroll"
content = content.replace(import_old, import_new)

column_old = """        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp)
        ) {"""

column_new = """        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState())
        ) {"""

content = content.replace(column_old, column_new)

weight_old = """            Spacer(modifier = Modifier.weight(1f))
            StitchSosButton(onSosClick = { sosTriggerHandler() })
            Spacer(modifier = Modifier.weight(1f))"""

weight_new = """            Spacer(modifier = Modifier.height(48.dp))
            StitchSosButton(onSosClick = { sosTriggerHandler() })
            Spacer(modifier = Modifier.height(48.dp))"""

content = content.replace(weight_old, weight_new)

with open("app/src/main/java/com/example/ui/screens/HomeScreen.kt", "w") as f:
    f.write(content)
