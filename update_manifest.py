import re

manifest_path = "app/src/main/AndroidManifest.xml"
with open(manifest_path, "r") as f:
    manifest_content = f.read()

permissions_to_add = """    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
    <uses-permission android:name="android.permission.VIBRATE" />
    
    <!-- Location -->
    <uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
    <uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
    <uses-permission android:name="android.permission.ACCESS_BACKGROUND_LOCATION" />
    
    <!-- Bluetooth -->
    <uses-permission android:name="android.permission.BLUETOOTH" />
    <uses-permission android:name="android.permission.BLUETOOTH_ADMIN" />
    <uses-permission android:name="android.permission.BLUETOOTH_SCAN" android:usesPermissionFlags="neverForLocation" />
    <uses-permission android:name="android.permission.BLUETOOTH_CONNECT" />
    
    <!-- Communications -->
    <uses-permission android:name="android.permission.SEND_SMS" />
    <uses-permission android:name="android.permission.CALL_PHONE" />
    <uses-permission android:name="android.permission.READ_CONTACTS" />
    
    <!-- Media / Hardware -->
    <uses-permission android:name="android.permission.CAMERA" />
    <uses-permission android:name="android.permission.RECORD_AUDIO" />
"""

# Replace existing permissions with the new comprehensive list
import re
new_manifest = re.sub(
    r'<uses-permission android:name="android\.permission\.INTERNET" />\s*<uses-permission android:name="android\.permission\.POST_NOTIFICATIONS" />\s*<uses-permission android:name="android\.permission\.VIBRATE" />',
    permissions_to_add.strip(),
    manifest_content
)

with open(manifest_path, "w") as f:
    f.write(new_manifest)
print("Updated manifest")
