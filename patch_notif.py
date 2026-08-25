import re

with open('app/src/main/java/com/example/service/NotificationService.kt', 'r') as f:
    content = f.read()

content = content.replace(
    'class NotificationService(',
    '@Suppress("DEPRECATION")\nclass NotificationService('
)

with open('app/src/main/java/com/example/service/NotificationService.kt', 'w') as f:
    f.write(content)
