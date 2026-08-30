import re

with open("app/src/main/AndroidManifest.xml", "r") as f:
    content = f.read()

advertise_perm = '    <uses-permission android:name="android.permission.BLUETOOTH_ADVERTISE" />\n'

if "BLUETOOTH_ADVERTISE" not in content:
    content = content.replace('    <!-- Bluetooth -->', f'    <!-- Bluetooth -->\n{advertise_perm}')

with open("app/src/main/AndroidManifest.xml", "w") as f:
    f.write(content)

