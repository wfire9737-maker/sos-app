import os

filepath = "app/src/main/java/com/example/ui/screens/PermissionsScreen.kt"
with open(filepath, "r") as f:
    content = f.read()

target = """import androidx.compose.material.icons.filled.Message"""
replacement = """import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Warning"""

if target in content:
    content = content.replace(target, replacement)
    with open(filepath, "w") as f:
        f.write(content)
    print("Fixed import")
else:
    print("Target not found")
