import re

with open("app/src/main/AndroidManifest.xml", "r") as f:
    content = f.read()

target = '<service android:name=".service.NearbyBleService" android:exported="false" />'
replacement = '<service android:name=".service.NearbyBleService" android:exported="false" android:foregroundServiceType="connectedDevice" />'

content = content.replace(target, replacement)

with open("app/src/main/AndroidManifest.xml", "w") as f:
    f.write(content)
