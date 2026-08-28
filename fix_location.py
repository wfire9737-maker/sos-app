import re

with open("app/src/main/java/com/example/service/LocationService.kt", "r") as f:
    content = f.read()

replacement = """        if (_currentLocation.value.favorites.isEmpty()) {
            updateLocationState { it.copy(favorites = emptyList()) }
        }"""
content = re.sub(r'        if \(_currentLocation\.value\.favorites\.isEmpty\(\)\) \{.*?        \}', replacement, content, flags=re.DOTALL)

with open("app/src/main/java/com/example/service/LocationService.kt", "w") as f:
    f.write(content)
