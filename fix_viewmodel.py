import re

with open('app/src/main/java/com/example/ui/GuardianViewModel.kt', 'r') as f:
    content = f.read()

replacement = '''
    fun startVoiceRecognition(context: Context) {
        setVoiceSosEnabled(true)
    }

    fun stopVoiceRecognition() {
        setVoiceSosEnabled(false)
    }
'''

content = re.sub(
    r'    fun startVoiceRecognition\(context: Context\) \{.*?\n    \}[\s\n]*fun stopVoiceRecognition\(\) \{.*?\n    \}',
    replacement.strip(),
    content,
    flags=re.DOTALL
)

with open('app/src/main/java/com/example/ui/GuardianViewModel.kt', 'w') as f:
    f.write(content)
