import os

filepath = "app/src/main/java/com/example/ui/navigation/NavGraph.kt"
with open(filepath, "r") as f:
    content = f.read()

target = """    val startDestination = Screen.Splash.route"""

replacement = """    val startDestination = Screen.Splash.route

    val fallState by viewModel.fallDetectionState.collectAsState()
    val countdown by viewModel.fallCountdown.collectAsState()"""

content = content.replace(target, replacement, 1)

with open(filepath, "w") as f:
    f.write(content)

print("Added variables")
