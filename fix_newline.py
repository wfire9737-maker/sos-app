import re

with open("app/src/main/java/com/example/ui/screens/DeveloperDashboardScreen.kt", "r") as f:
    text = f.read()

# Replace literal newline inside the string
text = re.sub(r'joinToString\("[\r\n]+"\)', 'joinToString("\\\\n")', text)

with open("app/src/main/java/com/example/ui/screens/DeveloperDashboardScreen.kt", "w") as f:
    f.write(text)
