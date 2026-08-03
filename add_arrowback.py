import os

files_to_fix = [
    "app/src/main/java/com/example/ui/screens/AboutScreen.kt",
    "app/src/main/java/com/example/ui/screens/HelpFaqScreen.kt",
    "app/src/main/java/com/example/ui/screens/PermissionsScreen.kt",
    "app/src/main/java/com/example/ui/screens/QRCodeScreen.kt",
    "app/src/main/java/com/example/ui/screens/SafeCheckInScreen.kt",
    "app/src/main/java/com/example/ui/screens/TrustedPlacesScreen.kt"
]

for filepath in files_to_fix:
    with open(filepath, "r") as f:
        content = f.read()
    
    if "import androidx.compose.material.icons.filled.ArrowBack" not in content and "import androidx.compose.material.icons.filled.*" not in content:
        content = content.replace("import androidx.compose.material.icons.Icons", "import androidx.compose.material.icons.Icons\nimport androidx.compose.material.icons.filled.ArrowBack")
        with open(filepath, "w") as f:
            f.write(content)
        print(f"Added ArrowBack to {filepath}")
