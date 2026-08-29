with open('app/src/main/AndroidManifest.xml', 'r') as f:
    content = f.read()

queries_intent = """    <queries>
        <intent>
            <action android:name="android.speech.RecognitionService" />
        </intent>
    </queries>"""

if '<queries>' not in content:
    content = content.replace('    <application', f"{queries_intent}\n\n    <application")
    with open('app/src/main/AndroidManifest.xml', 'w') as f:
        f.write(content)
