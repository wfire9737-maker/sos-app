import os

filepath = "app/src/main/java/com/example/ui/navigation/NavGraph.kt"
with open(filepath, "r") as f:
    content = f.read()

target = "onNavigateBack = { navController.popBackStack() }"
replacement = "onNavigateBack = { navController.navigateUp() }"

content = content.replace(target, replacement)

with open(filepath, "w") as f:
    f.write(content)

print("Updated NavGraph to use navigateUp()")
