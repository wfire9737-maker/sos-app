import re

with open("app/src/main/java/com/example/ui/screens/HomeScreen.kt", "r") as f:
    content = f.read()

content = re.sub(r'// Voice Command & Speech Recognition Section.*?VoiceCommandSection\(viewModel = viewModel\)\n\s*\}', '', content, flags=re.DOTALL)
content = re.sub(r'@Composable\s*fun VoiceCommandSection\(.*?\n\}\n\}', '', content, flags=re.DOTALL)

with open("app/src/main/java/com/example/ui/screens/HomeScreen.kt", "w") as f:
    f.write(content)
