import re

with open("app/src/main/AndroidManifest.xml", "r") as f:
    content = f.read()

service = '        <service android:name=".service.NearbyBleService" android:exported="false" />\n'

if "NearbyBleService" not in content:
    content = content.replace('        <service\n            android:name=".service.BleForegroundService"', f'{service}        <service\n            android:name=".service.BleForegroundService"')

with open("app/src/main/AndroidManifest.xml", "w") as f:
    f.write(content)
