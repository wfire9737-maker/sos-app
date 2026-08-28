import re

with open("app/src/main/java/com/example/ui/GuardianViewModel.kt", "r") as f:
    content = f.read()

content = content.replace("import android.content.Context\npackage com.example.ui", "package com.example.ui\nimport android.content.Context")

with open("app/src/main/java/com/example/ui/GuardianViewModel.kt", "w") as f:
    f.write(content)
