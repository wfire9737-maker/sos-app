import re

with open('app/src/main/java/com/example/ui/screens/HomeScreen.kt', 'r') as f:
    content = f.read()

# We need to remove the section starting with "Supported Voice Commands:"
# Let's find the exact block

pattern = r'\s*Spacer\(modifier = Modifier\.height\(12\.dp\)\)\s*Text\(\s*text = "Supported Voice Commands:".*?androidx\.compose\.foundation\.lazy\.LazyRow.*?\}\s*\}'
content = re.sub(pattern, '', content, flags=re.DOTALL)

with open('app/src/main/java/com/example/ui/screens/HomeScreen.kt', 'w') as f:
    f.write(content)
