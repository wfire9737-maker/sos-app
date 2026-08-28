import re

with open("app/src/main/java/com/example/ui/navigation/NavGraph.kt", "r") as f:
    content = f.read()

# Replace any multiple sequential onNavigateBack lines
content = re.sub(r'(onNavigateBack = \{ navController.navigateUp\(\) \},\s*)+', r'onNavigateBack = { navController.navigateUp() },\n                ', content)

with open("app/src/main/java/com/example/ui/navigation/NavGraph.kt", "w") as f:
    f.write(content)
