import re

with open('app/src/main/AndroidManifest.xml', 'r') as f:
    content = f.read()

# Add FOREGROUND_SERVICE_MICROPHONE permission if not present
if 'android.permission.FOREGROUND_SERVICE_MICROPHONE' not in content:
    content = content.replace('<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />',
                              '<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />\n    <uses-permission android:name="android.permission.FOREGROUND_SERVICE_MICROPHONE" />')

# Add service declaration
if '.service.VoiceSosForegroundService' not in content:
    service_decl = '''
        <service
            android:name=".service.VoiceSosForegroundService"
            android:foregroundServiceType="microphone"
            android:exported="false" />
    '''
    content = content.replace('</application>', service_decl + '\n    </application>')

with open('app/src/main/AndroidManifest.xml', 'w') as f:
    f.write(content)
