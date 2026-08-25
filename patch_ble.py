import re

with open('app/src/main/java/com/example/ble/BleManager.kt', 'r') as f:
    content = f.read()

# Prepend @Suppress("DEPRECATION", "MissingPermission") to BleManager class
content = content.replace(
    'class BleManager(private val context: Context)',
    '@Suppress("DEPRECATION", "MissingPermission")\nclass BleManager(private val context: Context)'
)

with open('app/src/main/java/com/example/ble/BleManager.kt', 'w') as f:
    f.write(content)
