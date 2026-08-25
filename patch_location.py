import re

with open('app/src/main/java/com/example/service/LocationService.kt', 'r') as f:
    content = f.read()

content = content.replace(
    'class LocationService(',
    '@Suppress("DEPRECATION")\nclass LocationService('
)

with open('app/src/main/java/com/example/service/LocationService.kt', 'w') as f:
    f.write(content)
