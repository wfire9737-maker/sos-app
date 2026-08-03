import os

filepath = "app/src/main/AndroidManifest.xml"
with open(filepath, "r") as f:
    content = f.read()

if "<service android:name=\".service.LocationForegroundService\"" not in content:
    target = """        <service
            android:name=".service.GuardianFirebaseMessagingService"
            android:exported="false">
            <intent-filter>
                <action android:name="com.google.firebase.MESSAGING_EVENT" />
            </intent-filter>
        </service>"""
    
    replacement = target + """
        <service
            android:name=".service.LocationForegroundService"
            android:foregroundServiceType="location"
            android:exported="false" />"""
    
    content = content.replace(target, replacement)
    with open(filepath, "w") as f:
        f.write(content)
    print("Added LocationForegroundService to Manifest")
else:
    print("Already in manifest")
